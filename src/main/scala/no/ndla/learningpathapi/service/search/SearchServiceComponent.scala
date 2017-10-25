/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import com.google.gson.{JsonElement, JsonObject}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.ElasticClientComponent
import no.ndla.learningpathapi.model.api.{Copyright, LearningPathSummaryV2, License, Error}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.search.SearchableLearningPath
import no.ndla.network.ApplicationUrl
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.{FieldSortBuilder, SortBuilders, SortOrder}
import org.json4s.native.Serialization._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait SearchServiceComponent extends LazyLogging {
  this: SearchIndexServiceComponent with ElasticClientComponent with SearchConverterServiceComponent =>
  val searchService: SearchService

  class SearchService {
    /*
    //TODO: Remove
      def getHits(response: JestSearchResult): Seq[LearningPathSummary] = {
        var resultList = Seq[LearningPathSummary]()
        response.getTotal match {
          case count: Integer if count > 0 => {
            val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
            val iterator = resultArray.iterator()
            while (iterator.hasNext) {
              resultList = resultList :+ hitAsLearningPathSummary(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject])
            }
            resultList
          }
          case _ => Seq()
        }
      }
      */

    def getHitsV2(response: JestSearchResult, language: String): Seq[LearningPathSummaryV2] = {
      var resultList = Seq[LearningPathSummaryV2]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while (iterator.hasNext) {
            resultList = resultList :+ hitAsLearningPathSummaryV2(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject], language)
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsLearningPathSummaryV2(jsonObject: JsonObject, language: String): LearningPathSummaryV2 = {
      implicit val formats = org.json4s.DefaultFormats
      searchConverterService.asApiLearningPathSummaryV2(read[SearchableLearningPath](jsonObject.toString), language)
    }

    def all(withIdIn: List[Long], taggedWith: Option[String], sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      executeSearch(
        QueryBuilders.boolQuery(),
        withIdIn,
        taggedWith,
        sort,
        language.getOrElse(LearningpathApiProperties.DefaultLanguage),
        page,
        pageSize)
    }

    def allV2(withIdIn: List[Long], taggedWith: Option[String], sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {

      val fullQuery = language match {
        case None => QueryBuilders.boolQuery()
        case Some(language) => {
          val titleSearch = QueryBuilders.existsQuery(s"titles.$language")
          val descSearch = QueryBuilders.existsQuery(s"descriptions.$language")

          QueryBuilders.boolQuery()
            .should(QueryBuilders.nestedQuery("titles", titleSearch, ScoreMode.Avg))
            .should(QueryBuilders.nestedQuery("descriptions", descSearch, ScoreMode.Avg))
        }
      }

      executeSearch(
        fullQuery,
        withIdIn,
        taggedWith,
        sort,
        language.getOrElse(LearningpathApiProperties.DefaultLanguage),
        page,
        pageSize)
    }

    def matchingQuery(withIdIn: List[Long], query: String, taggedWith: Option[String], language: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val searchLanguage = language.getOrElse(LearningpathApiProperties.DefaultLanguage)

      val titleSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"titles.$searchLanguage")
      val descSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"descriptions.$searchLanguage")
      val stepTitleSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"learningsteps.titles.$searchLanguage")
      val stepDescSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"learningsteps.descriptions.$searchLanguage")
      val tagSearch = QueryBuilders.simpleQueryStringQuery(query).field(s"tags.$searchLanguage")
      val authorSearch = QueryBuilders.simpleQueryStringQuery(query).field("author")

      val fullQuery = QueryBuilders.boolQuery()
        .must(
          QueryBuilders.boolQuery()
            .should(QueryBuilders.nestedQuery("titles", titleSearch, ScoreMode.Avg)).boost(2)
            .should(QueryBuilders.nestedQuery("descriptions", descSearch, ScoreMode.Avg)).boost(2)
            .should(QueryBuilders.nestedQuery("learningsteps.titles", stepTitleSearch, ScoreMode.Avg)).boost(1)
            .should(QueryBuilders.nestedQuery("learningsteps.descriptions", stepDescSearch, ScoreMode.Avg)).boost(1)
            .should(QueryBuilders.nestedQuery("tags", tagSearch, ScoreMode.Avg)).boost(2)
            .should(authorSearch).boost(1))

      executeSearch(fullQuery, withIdIn, taggedWith, sort, searchLanguage, page, pageSize)
    }

    def executeSearch(queryBuilder: BoolQueryBuilder, withIdIn: List[Long], taggedWith: Option[String], sort: Sort.Value, language: String, page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val tagFilteredSearch = taggedWith match {
        case None => queryBuilder
        case Some(tag) => queryBuilder.filter(QueryBuilders.nestedQuery("tags", QueryBuilders.termQuery(s"tags.$language.raw", tag), ScoreMode.None))
      }

      val idFilteredSearch = withIdIn match {
        case head :: tail => tagFilteredSearch.filter(QueryBuilders.idsQuery(LearningpathApiProperties.SearchDocument).addIds(head.toString :: tail.map(_.toString):_*))
        case Nil => tagFilteredSearch
      }

      val searchQuery = new SearchSourceBuilder().query(idFilteredSearch).sort(getSortDefinition(sort, language))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(searchQuery.toString)
        .addIndex(LearningpathApiProperties.SearchIndex)
        .setParameter(Parameters.SIZE, numResults)
        .setParameter("from", startAt)

      val requestedResultWindow = page.getOrElse(1)*numResults
      if(requestedResultWindow > LearningpathApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${LearningpathApiProperties.ElasticSearchIndexMaxResultWindow}, user requested ${requestedResultWindow}")
        throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }

      jestClient.execute(request.build()) match {
        case Success(response) => SearchResult(response.getTotal.toLong, page.getOrElse(1), numResults, language, response)
        case Failure(f) => errorHandler(Failure(f))
      }
    }

    def countDocuments(): Int = {
      val ret = jestClient.execute(
        new Count.Builder().addIndex(LearningpathApiProperties.SearchIndex).build()
      ).map(result => result.getCount.toInt)
      ret.getOrElse(0)
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSortBuilder = {
      sort match {
        case (Sort.ByDurationAsc) => SortBuilders.fieldSort("duration").order(SortOrder.ASC).missing("_last")
        case (Sort.ByDurationDesc) => SortBuilders.fieldSort("duration").order(SortOrder.DESC).missing("_last")
        case (Sort.ByLastUpdatedAsc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByTitleAsc) => SortBuilders.fieldSort(s"titles.$language.raw").setNestedPath("titles").order(SortOrder.ASC).missing("_last")
        case (Sort.ByTitleDesc) => SortBuilders.fieldSort(s"titles.$language.raw").setNestedPath("titles").order(SortOrder.DESC).missing("_last")
        case (Sort.ByRelevanceAsc) => SortBuilders.fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => SortBuilders.fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByIdAsc) => SortBuilders.fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc) => SortBuilders.fieldSort("id").order(SortOrder.DESC).missing("_last")
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
          e.getResponse.getResponseCode match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index ${LearningpathApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index ${LearningpathApiProperties.SearchIndex} not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getResponse.getErrorMessage)
              throw new ElasticsearchException(s"Unable to execute search in ${LearningpathApiProperties.SearchIndex}", e.getResponse.getErrorMessage)
            }
          }

        }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        searchIndexService.indexDocuments
      }

      f onFailure { case t => logger.warn("Unable to create index: " + t.getMessage, t) }
      f onSuccess {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

    def getOrNone(hit: JsonObject, fieldPath: String): Option[JsonElement] = {
      Option(hit.get(fieldPath))
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

    def findValueWithPathAndLanguage(hit: JsonObject, fieldPath: String, language: String): String = {
      import scala.collection.JavaConversions._

      hit.get(fieldPath).getAsJsonObject.entrySet().to[Seq]find(entry => entry.getKey == language) match {
        case Some(element) => element.getValue.getAsString
        case None => ""
      }
    }
  }

}
