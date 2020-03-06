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

    def allV2(withIdIn: List[Long],
              taggedWith: Option[String],
              sort: Sort.Value,
              searchLanguage: String,
              page: Option[Int],
              pageSize: Option[Int],
              fallback: Boolean,
              verificationStatus: Option[String]): Try[SearchResult] = {
      val language = if (searchLanguage == Language.AllLanguages || fallback) "*" else searchLanguage
      val fullQuery = language match {
        case "*" => boolQuery()
        case lang =>
          val titleSearch = existsQuery(s"titles.$lang")
          val descSearch = existsQuery(s"descriptions.$lang")

          boolQuery()
            .should(
              nestedQuery("titles", titleSearch).scoreMode(ScoreMode.Avg),
              nestedQuery("descriptions", descSearch).scoreMode(ScoreMode.Avg)
            )
      }

      executeSearch(
        fullQuery,
        withIdIn,
        taggedWith,
        List.empty,
        sort,
        searchLanguage,
        page,
        pageSize,
        fallback,
        verificationStatus
      )
    }

    def containsPath(paths: List[String]): Try[SearchResult] = {
      executeSearch(
        boolQuery(),
        List.empty,
        None,
        paths,
        Sort.ByTitleAsc,
        Language.AllLanguages,
        None,
        None,
        false,
        None
      )
    }

    def matchingQuery(withIdIn: List[Long],
                      query: String,
                      taggedWith: Option[String],
                      searchLanguage: String,
                      sort: Sort.Value,
                      page: Option[Int],
                      pageSize: Option[Int],
                      fallback: Boolean,
                      verificationStatus: Option[String]): Try[SearchResult] = {
      val language =
        if (searchLanguage == Language.AllLanguages || fallback) "*"
        else searchLanguage

      val titleSearch = simpleStringQuery(query).field(s"titles.$language", 2)
      val descSearch = simpleStringQuery(query).field(s"descriptions.$language", 2)
      val stepTitleSearch = simpleStringQuery(query).field(s"learningsteps.titles.$language", 1)
      val stepDescSearch = simpleStringQuery(query).field(s"learningsteps.descriptions.$language", 1)
      val tagSearch = simpleStringQuery(query).field(s"tags.$language", 2)
      val authorSearch = simpleStringQuery(query).field("author", 1)

      val fullQuery = boolQuery()
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

      executeSearch(fullQuery,
                    withIdIn,
                    taggedWith,
                    List.empty,
                    sort,
                    language,
                    page,
                    pageSize,
                    fallback,
                    verificationStatus)
    }

    private def executeSearch(queryBuilder: BoolQuery,
                              withIdIn: List[Long],
                              taggedWith: Option[String],
                              withPaths: List[String],
                              sort: Sort.Value,
                              language: String,
                              page: Option[Int],
                              pageSize: Option[Int],
                              fallback: Boolean,
                              verificationStatus: Option[String]) = {
      val tagFilter: Option[NestedQuery] = taggedWith.map(
        tag => nestedQuery("tags", termQuery(s"tags.$language.raw", tag)).scoreMode(ScoreMode.None)
      )
      val idFilter = if (withIdIn.isEmpty) None else Some(idsQuery(withIdIn))
      val pathFilter = pathsFilterQuery(withPaths)
      val (languageFilter, searchLanguage) = language match {
        case "" | Language.AllLanguages =>
          (None, "*")
        case lang =>
          if (fallback) (None, "*") else (Some(nestedQuery("titles").query(existsQuery(s"titles.$lang"))), lang)
      }

      val verificationStatusFilter = verificationStatus.map(status => termQuery("verificationStatus", status))

      val filters = List(tagFilter, idFilter, pathFilter, languageFilter, verificationStatusFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = page.getOrElse(1) * numResults
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
          .sortBy(getSortDefinition(sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt != 0) { searchToExecute } else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(page.getOrElse(1)),
                numResults,
                if (language == "*") Language.AllLanguages else language,
                getHitsV2(response.result, language),
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
              fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw")
                .nestedPath("titles")
                .order(SortOrder.ASC)
                .missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" | Language.AllLanguages =>
              fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw")
                .nestedPath("titles")
                .order(SortOrder.DESC)
                .missing("_last")
          }
        case Sort.ByDurationAsc =>
          fieldSort("duration").order(SortOrder.ASC).missing("_last")
        case Sort.ByDurationDesc =>
          fieldSort("duration").order(SortOrder.DESC).missing("_last")
        case Sort.ByLastUpdatedAsc =>
          fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case Sort.ByLastUpdatedDesc =>
          fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case Sort.ByRelevanceAsc  => fieldSort("_score").order(SortOrder.ASC)
        case Sort.ByRelevanceDesc => fieldSort("_score").order(SortOrder.DESC)
        case Sort.ByIdAsc =>
          fieldSort("id").order(SortOrder.ASC).missing("_last")
        case Sort.ByIdDesc =>
          fieldSort("id").order(SortOrder.DESC).missing("_last")
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
