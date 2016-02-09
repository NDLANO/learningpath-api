package no.ndla.learningpathapi.integration

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.business.{LearningPathSearch, SearchIndexer}
import no.ndla.learningpathapi.model.Sort
import no.ndla.learningpathapi.service.ModelConverters.asApiLearningPathSummary
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.indices.IndexMissingException
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.native.Serialization.read

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ElasticLearningPathSearch(client: ElasticClient) extends LearningPathSearch with LazyLogging {

  implicit object ContentHitAs extends HitAs[LearningPathSummary] {
    override def as(hit: RichSearchHit): LearningPathSummary = {
      implicit val formats = org.json4s.DefaultFormats
      asApiLearningPathSummary(read[LearningPath](hit.sourceAsString))
    }
  }

  override def all(sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
    val theSearch = search in LearningpathApiProperties.SearchIndex -> LearningpathApiProperties.SearchDocument
    executeSearch(theSearch, sort, language, page, pageSize)
  }

  override def matchingQuery(query: Iterable[String], language: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int]): SearchResult = {
    val titleSearch = new ListBuffer[QueryDefinition]
    titleSearch += matchQuery("title.title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => titleSearch += termQuery("title.language", lang))

    val descSearch = new ListBuffer[QueryDefinition]
    descSearch += matchQuery("description.description", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => descSearch += termQuery("description.language", lang))

    val stepTitleSearch = new ListBuffer[QueryDefinition]
    stepTitleSearch += matchQuery("learningsteps.title.title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => stepTitleSearch += termQuery("learningsteps.title.language", lang))

    val stepDescSearch = new ListBuffer[QueryDefinition]
    stepDescSearch += matchQuery("learningsteps.description.description", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => stepDescSearch += termQuery("learningsteps.description.language", lang))

    val tagSearch = new ListBuffer[QueryDefinition]
    tagSearch += matchQuery("tags.tag", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => tagSearch += termQuery("tags.language", lang))

    val authorSearch = new ListBuffer[QueryDefinition]
    authorSearch += matchQuery("author.name", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)

    val theSearch = search in LearningpathApiProperties.SearchIndex -> LearningpathApiProperties.SearchDocument query {
      bool {
        should (
          nestedQuery("title").query {bool {must (titleSearch.toList)}},
          nestedQuery("description").query {bool {must (descSearch.toList)}},
          nestedQuery("learningsteps.title").query {bool {must (stepTitleSearch.toList)}},
          nestedQuery("learningsteps.description").query {bool {must (stepDescSearch.toList)}},
          nestedQuery("tags").query {bool {must (tagSearch.toList)}},
          nestedQuery("author").query {bool {must (authorSearch.toList)}}
        )
      }
    }

    executeSearch(theSearch, sort, language, page, pageSize)
  }

  def getSortDefinition(sort: Sort.Value, language: Option[String]): SortDefinition = {
    val sortingLanguage = language.getOrElse(LearningpathApiProperties.DefaultLanguage)

    sort match {
      case (Sort.ByDurationAsc) => fieldSort("duration").order(SortOrder.ASC).missing("_last")
      case (Sort.ByDurationDesc) => fieldSort("duration").order(SortOrder.DESC).missing("_last")
      case (Sort.ByLastUpdatedAsc) => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
      case (Sort.ByLastUpdatedDesc) => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
      case (Sort.ByTitleAsc) => fieldSort("title.title.raw").nestedPath("title").nestedFilter(termFilter("title.language", sortingLanguage)).order(SortOrder.ASC).missing("_last")
      case (Sort.ByTitleDesc) => fieldSort("title.title.raw").nestedPath("title").nestedFilter(termFilter("title.language", sortingLanguage)).order(SortOrder.DESC).missing("_last")
      case (Sort.ByRelevanceAsc) => fieldSort("_score").order(SortOrder.ASC)
      case (Sort.ByRelevanceDesc) => fieldSort("_score").order(SortOrder.DESC)
    }
  }

  def executeSearch(search: SearchDefinition, sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
    val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
    try {

      search.sort(getSortDefinition(sort, language))

      val actualSearch = search start startAt limit numResults
      val response = client.execute {
        actualSearch
      }.await

      SearchResult(response.getHits.getTotalHits, page.getOrElse(1), numResults, response.as[LearningPathSummary])
    } catch {
      case e: RemoteTransportException => errorHandler(e.getCause)
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
      SearchIndexer.indexDocuments()
    }
    f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
  }
}
