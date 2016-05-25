package no.ndla.learningpathapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.integration.ElasticClientComponent
import no.ndla.learningpathapi.model.api.{LearningPath, LearningPathSummary, SearchResult}
import no.ndla.learningpathapi.model.domain.Sort
import no.ndla.learningpathapi.model.search.SearchableLearningPath
import no.ndla.learningpathapi.service.ConverterServiceComponent
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.mapping.ISO639Mapping
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.indices.IndexMissingException
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.native.Serialization._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SearchServiceComponent extends LazyLogging {
  this: SearchIndexBuilderServiceComponent with ElasticClientComponent with SearchConverterServiceComponent =>
  val searchService: SearchService

  class SearchService {
    implicit object ContentHitAs extends HitAs[LearningPathSummary] {
      override def as(hit: RichSearchHit): LearningPathSummary = {
        implicit val formats = org.json4s.DefaultFormats
        searchConverterService.asApiLearningPathSummary(read[SearchableLearningPath](hit.sourceAsString))
      }
    }

    def all(sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val theSearch = search in LearningpathApiProperties.SearchIndex -> LearningpathApiProperties.SearchDocument
      executeSearch(theSearch, sort, language, page, pageSize)
    }

    def matchingQuery(query: Iterable[String], language: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val searchLanguage = language.getOrElse(ISO639Mapping.NORWEGIAN_BOKMAL)

      val titleSearch = matchQuery(s"titles.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val descSearch = matchQuery(s"descriptions.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val stepTitleSearch = matchQuery(s"learningsteps.titles.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val stepDescSearch = matchQuery(s"learningsteps.descriptions.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val tagSearch = matchQuery(s"tags.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val authorSearch = matchQuery("author", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)

      val theSearch = search in LearningpathApiProperties.SearchIndex -> LearningpathApiProperties.SearchDocument query {
        bool {
          should (
            nestedQuery("titles").query(titleSearch),
            nestedQuery("descriptions").query(descSearch),
            nestedQuery("learningsteps.titles").query(stepTitleSearch),
            nestedQuery("learningsteps.descriptions").query(stepDescSearch),
            nestedQuery("tags").query(tagSearch),
            authorSearch
          )
        }
      }

      executeSearch(theSearch, sort, language, page, pageSize)
    }

    def executeSearch(search: SearchDefinition, sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      try {

        search.sort(getSortDefinition(sort, language))

        val actualSearch = search start startAt limit numResults
        logger.debug("Running query = {}", actualSearch._builder.toString)

        val response = elasticClient.execute {
          actualSearch
        }.await

        SearchResult(response.getHits.getTotalHits, page.getOrElse(1), numResults, response.as[LearningPathSummary])
      } catch {
        case e: RemoteTransportException => errorHandler(e.getCause)
      }
    }

    def getSortDefinition(sort: Sort.Value, language: Option[String]): SortDefinition = {
      val sortingLanguage = language.getOrElse(LearningpathApiProperties.DefaultLanguage)

      sort match {
        case (Sort.ByDurationAsc) => fieldSort("duration").order(SortOrder.ASC).missing("_last")
        case (Sort.ByDurationDesc) => fieldSort("duration").order(SortOrder.DESC).missing("_last")
        case (Sort.ByLastUpdatedAsc) => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByTitleAsc) => fieldSort(s"titles.$sortingLanguage.raw").order(SortOrder.ASC).missing("_last")
        case (Sort.ByTitleDesc) => fieldSort(s"titles.$sortingLanguage.raw").order(SortOrder.DESC).missing("_last")
        case (Sort.ByRelevanceAsc) => fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => fieldSort("_score").order(SortOrder.DESC)
      }
    }

    def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(LearningpathApiProperties.MaxPageSize) else LearningpathApiProperties.DefaultPageSize
        case None => LearningpathApiProperties.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None => 0
      }

      (startAt, numResults)
    }

    def errorHandler(exception: Throwable) = {
      exception match {
        case ex: IndexMissingException =>
          logger.error(ex.getDetailedMessage)
          scheduleIndexDocuments()
          throw ex
        case _ => throw exception
      }
    }

    def scheduleIndexDocuments() = {
      val f = Future {
        searchIndexBuilderService.indexDocuments()
      }
      f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
    }
  }
}
