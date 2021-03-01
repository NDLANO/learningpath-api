/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import java.util.concurrent.Executors

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, NestedQuery}
import com.sksamuel.elastic4s.searches.sort.SortOrder
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.LearningpathApiProperties.{
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive
}
import no.ndla.learningpathapi.integration.Elastic4sClient
import no.ndla.learningpathapi.model.api.{Copyright, Error, LearningPathSummaryV2, License}
import no.ndla.learningpathapi.model.domain.{Sort, _}
import no.ndla.learningpathapi.model.search.SearchableLearningPath
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

trait SearchService extends LazyLogging {
  this: SearchIndexService with Elastic4sClient with SearchConverterServiceComponent =>
  val searchService: SearchService

  class SearchService {

    def scroll(scrollId: String, language: String): Try[SearchResult] =
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .map(response => {
          val hits = getHitsV2(response.result, language)

          SearchResult(
            totalCount = response.result.totalHits,
            page = None,
            pageSize = response.result.hits.hits.length,
            language = if (language == "*") Language.AllLanguages else language,
            results = hits,
            scrollId = response.result.scrollId
          )
        })

    def getHitsV2(response: SearchResponse, language: String): Seq[LearningPathSummaryV2] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                searchConverterService
                  .getLanguageFromHit(result)
                  .getOrElse(language)
              case _ => language
            }

            hitAsLearningPathSummaryV2(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def hitAsLearningPathSummaryV2(hitString: String, language: String): LearningPathSummaryV2 = {
      implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
      searchConverterService.asApiLearningPathSummaryV2(read[SearchableLearningPath](hitString), language)
    }

    def containsPath(paths: List[String]): Try[SearchResult] = {
      val settings = SearchSettings(
        query = None,
        withIdIn = List.empty,
        withPaths = paths,
        taggedWith = None,
        searchLanguage = Language.AllLanguages,
        sort = Sort.ByTitleAsc,
        page = None,
        pageSize = None,
        fallback = false,
        verificationStatus = None,
        shouldScroll = false,
        status = List(
          LearningPathStatus.PUBLISHED,
          LearningPathStatus.SUBMITTED,
          LearningPathStatus.UNLISTED
        )
      )

      executeSearch(boolQuery(), settings)
    }

    def matchingQuery(settings: SearchSettings): Try[SearchResult] = {
      val language =
        if (settings.searchLanguage == Language.AllLanguages || settings.fallback) "*"
        else settings.searchLanguage

      val fullQuery = settings.query match {
        case Some(query) =>
          val titleSearch = simpleStringQuery(query).field(s"titles.$language", 2)
          val descSearch = simpleStringQuery(query).field(s"descriptions.$language", 2)
          val stepTitleSearch = simpleStringQuery(query).field(s"learningsteps.titles.$language", 1)
          val stepDescSearch = simpleStringQuery(query).field(s"learningsteps.descriptions.$language", 1)
          val tagSearch = simpleStringQuery(query).field(s"tags.$language", 2)
          val authorSearch = simpleStringQuery(query).field("author", 1)
          boolQuery()
            .must(
              boolQuery()
                .should(
                  nestedQuery("titles", titleSearch),
                  nestedQuery("descriptions", descSearch),
                  nestedQuery("learningsteps.titles", stepTitleSearch),
                  nestedQuery("learningsteps.descriptions", stepDescSearch),
                  nestedQuery("tags", tagSearch),
                  authorSearch
                )
            )
        case None if language == "*" => boolQuery()
        case _ =>
          val titleSearch = existsQuery(s"titles.$language")
          val descSearch = existsQuery(s"descriptions.$language")
          boolQuery()
            .should(
              nestedQuery("titles", titleSearch).scoreMode(ScoreMode.Avg),
              nestedQuery("descriptions", descSearch).scoreMode(ScoreMode.Avg)
            )
      }

      executeSearch(fullQuery, settings)
    }

    private def getStatusFilter(settings: SearchSettings) = settings.status match {
      case Nil      => Some(termQuery("status", "PUBLISHED"))
      case statuses => Some(termsQuery("status", statuses))
    }

    private def executeSearch(queryBuilder: BoolQuery, settings: SearchSettings) = {
      val tagFilter: Option[NestedQuery] = settings.taggedWith.map(
        tag => nestedQuery("tags", termQuery(s"tags.${settings.searchLanguage}.raw", tag)).scoreMode(ScoreMode.None)
      )
      val idFilter = if (settings.withIdIn.isEmpty) None else Some(idsQuery(settings.withIdIn))
      val pathFilter = pathsFilterQuery(settings.withPaths)
      val (languageFilter, searchLanguage) = settings.searchLanguage match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          if (settings.fallback) (None, "*")
          else (Some(nestedQuery("titles").query(existsQuery(s"titles.$lang"))), lang)
      }

      val verificationStatusFilter = settings.verificationStatus.map(status => termQuery("verificationStatus", status))

      val statusFilter = getStatusFilter(settings)

      val filters = List(
        tagFilter,
        idFilter,
        pathFilter,
        languageFilter,
        verificationStatusFilter,
        statusFilter
      )

      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException(Error.WindowTooLargeError.description))
      } else {
        val searchToExecute = search(LearningpathApiProperties.SearchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .highlighting(highlight("*"))
          .sortBy(getSortDefinition(settings.sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
          } else { searchToExecute.explain(true) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(settings.page.getOrElse(1)),
                numResults,
                if (searchLanguage == "*") Language.AllLanguages else settings.searchLanguage,
                getHitsV2(response.result, settings.searchLanguage),
                response.result.scrollId
              ))
          case Failure(ex) =>
            errorHandler(ex)
        }

      }
    }

    def pathsFilterQuery(paths: List[String]): Option[NestedQuery] = {
      if (paths.isEmpty) None
      else {
        Some(
          nestedQuery("learningsteps",
                      boolQuery().should(paths.map(p => wildcardQuery("learningsteps.embedUrl", s"*$p")))))
      }
    }

    def countDocuments(): Long = {
      val response = e4sClient.execute {
        catCount(LearningpathApiProperties.SearchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    private def getSortDefinition(sort: Sort.Value, language: String) = {
      val sortLanguage = language match {
        case Language.NoLanguage => Language.DefaultLanguage
        case _                   => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case "*" | Language.AllLanguages =>
              fieldSort("defaultTitle").order(SortOrder.Asc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw")
                .nestedPath("titles")
                .order(SortOrder.Asc)
                .missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" | Language.AllLanguages =>
              fieldSort("defaultTitle").order(SortOrder.Desc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw")
                .nestedPath("titles")
                .order(SortOrder.Desc)
                .missing("_last")
          }
        case Sort.ByDurationAsc =>
          fieldSort("duration").order(SortOrder.Asc).missing("_last")
        case Sort.ByDurationDesc =>
          fieldSort("duration").order(SortOrder.Desc).missing("_last")
        case Sort.ByLastUpdatedAsc =>
          fieldSort("lastUpdated").order(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc =>
          fieldSort("lastUpdated").order(SortOrder.Desc).missing("_last")
        case Sort.ByRelevanceAsc  => fieldSort("_score").order(SortOrder.Asc)
        case Sort.ByRelevanceDesc => fieldSort("_score").order(SortOrder.Desc)
        case Sort.ByIdAsc =>
          fieldSort("id").order(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc =>
          fieldSort("id").order(SortOrder.Desc).missing("_last")
      }
    }

    def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(LearningpathApiProperties.MaxPageSize)
          else LearningpathApiProperties.DefaultPageSize
        case None => LearningpathApiProperties.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None     => 0
      }

      (startAt, numResults)
    }

    private def errorHandler[T](exception: Throwable): Failure[T] = {
      exception match {
        case e: NdlaSearchException =>
          e.rf.status match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index ${LearningpathApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              Failure(
                new IndexNotFoundException(
                  s"Index ${LearningpathApiProperties.SearchIndex} not found. Scheduling a reindex"))
            case _ =>
              logger.error(e.getMessage)
              Failure(
                new ElasticsearchException(s"Unable to execute search in ${LearningpathApiProperties.SearchIndex}",
                                           e.getMessage))
          }
        case t => Failure(t)
      }
    }

    private def scheduleIndexDocuments(): Unit = {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val f = Future {
        searchIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

    def readToApiCopyright(copyright: Copyright): Copyright = {
      Copyright(License(
                  copyright.license.license,
                  copyright.license.description,
                  copyright.license.url
                ),
                copyright.contributors)
    }
  }

}
