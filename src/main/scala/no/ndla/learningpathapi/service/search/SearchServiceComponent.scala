/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import java.util.concurrent.Executors

import com.sksamuel.elastic4s.http.search.SearchResponse
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.Elastic4sClient
import no.ndla.learningpathapi.model.api.{Copyright, Error, LearningPathSummaryV2, License, SearchResultV2}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.search.SearchableLearningPath
import no.ndla.network.ApplicationUrl
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.sksamuel.elastic4s.searches.sort.SortOrder
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.native.Serialization._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait SearchServiceComponent extends LazyLogging {
  this: SearchIndexServiceComponent with Elastic4sClient with SearchConverterServiceComponent =>
  val searchService: SearchService

  class SearchService {
    def getHitsV2(response: SearchResponse, language: String): Seq[LearningPathSummaryV2] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }

            hitAsLearningPathSummaryV2(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def hitAsLearningPathSummaryV2(hitString: String, language: String): LearningPathSummaryV2 = {
      implicit val formats = org.json4s.DefaultFormats
      searchConverterService.asApiLearningPathSummaryV2(read[SearchableLearningPath](hitString), language)
    }

    def allV2(withIdIn: List[Long], taggedWith: Option[String], sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResultV2 = {
      val searchLanguage = language match {
        case None | Some(Language.AllLanguages) => "*"
        case Some(lang) => lang
      }

      val fullQuery = searchLanguage match {
        case "*" => boolQuery()
        case lang => {
          val titleSearch = existsQuery(s"titles.$lang")
          val descSearch = existsQuery(s"descriptions.$lang")

          boolQuery()
            .should(
              nestedQuery("titles", titleSearch).scoreMode(ScoreMode.Avg),
              nestedQuery("descriptions", descSearch).scoreMode(ScoreMode.Avg)
            )
        }
      }

      executeSearch(
        fullQuery,
        withIdIn,
        taggedWith,
        sort,
        searchLanguage,
        page,
        pageSize
      )
    }

    def matchingQuery(withIdIn: List[Long], query: String, taggedWith: Option[String], language: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int]): SearchResultV2 = {
      val searchLanguage = language match {
        case None | Some(Language.AllLanguages) => "*"
        case Some(lang) => lang
      }

      val titleSearch = simpleStringQuery(query).field(s"titles.$searchLanguage", 2)
      val descSearch = simpleStringQuery(query).field(s"descriptions.$searchLanguage", 2)
      val stepTitleSearch = simpleStringQuery(query).field(s"learningsteps.titles.$searchLanguage", 1)
      val stepDescSearch = simpleStringQuery(query).field(s"learningsteps.descriptions.$searchLanguage", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$searchLanguage", 2)
      val authorSearch = simpleStringQuery(query).field("author", 1)

      val hi = highlight("*").preTag("").postTag("").numberOfFragments(0)

      val fullQuery = boolQuery()
        .must(
          boolQuery()
            .should(
              nestedQuery("titles", titleSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("titles").highlighting(hi)),
              nestedQuery("descriptions", descSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("descriptions").highlighting(hi)),
              nestedQuery("learningsteps.titles", stepTitleSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("learningsteps.titles").highlighting(hi)),
              nestedQuery("learningsteps.descriptions", stepDescSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("learningsteps.descriptions").highlighting(hi)),
              nestedQuery("tags", tagSearch).scoreMode(ScoreMode.Avg).boost(1).inner(innerHits("tags").highlighting(hi)),
              authorSearch
            )
        )

      executeSearch(fullQuery, withIdIn, taggedWith, sort, searchLanguage, page, pageSize)
    }

    def executeSearch(queryBuilder: BoolQueryDefinition, withIdIn: List[Long], taggedWith: Option[String], sort: Sort.Value, language: String, page: Option[Int], pageSize: Option[Int]): SearchResultV2 = {
      val tagFilter = taggedWith match {
        case None => None
        case Some(tag) => Some(nestedQuery("tags", termQuery(s"tags.$language.raw", tag)).scoreMode(ScoreMode.None))
      }
      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))

      val filters = List(tagFilter, idFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = page.getOrElse(1) * numResults
      if (requestedResultWindow > LearningpathApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${LearningpathApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }

      e4sClient.execute {
        search(LearningpathApiProperties.SearchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .sortBy(getSortDefinition(sort, language))
      } match {
        case Success(response) =>
          SearchResultV2(response.result.totalHits, page.getOrElse(1), numResults, if (language == "*") Language.AllLanguages else language, getHitsV2(response.result, language))
        case Failure(ex) =>
          errorHandler(Failure(ex))
      }
    }

    def countDocuments(): Long = {
      val response = e4sClient.execute {
        catCount(LearningpathApiProperties.SearchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_) => 0
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String) = {
      val sortLanguage = language match {
        case Language.NoLanguage => Language.DefaultLanguage
        case _ => language
      }

      sort match {
        case (Sort.ByTitleAsc) =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _ => fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").order(SortOrder.ASC).missing("_last")
          }
        case (Sort.ByTitleDesc) =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _ => fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").order(SortOrder.DESC).missing("_last")
          }
        case (Sort.ByDurationAsc) => fieldSort("duration").order(SortOrder.ASC).missing("_last")
        case (Sort.ByDurationDesc) => fieldSort("duration").order(SortOrder.DESC).missing("_last")
        case (Sort.ByLastUpdatedAsc) => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByRelevanceAsc) => fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByIdAsc) => fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc) => fieldSort("id").order(SortOrder.DESC).missing("_last")
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

    private def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: NdlaSearchException) => {
          e.rf.status match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index ${LearningpathApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index ${LearningpathApiProperties.SearchIndex} not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getMessage)
              throw new ElasticsearchException(s"Unable to execute search in ${LearningpathApiProperties.SearchIndex}", e.getMessage)
            }
          }

        }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments(): Unit = {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        searchIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

    def readToApiCopyright(copyright: Copyright): Copyright = {
      Copyright(
        License(
          copyright.license.license,
          copyright.license.description,
          copyright.license.url
        ),
        copyright.contributors)
    }
  }

}
