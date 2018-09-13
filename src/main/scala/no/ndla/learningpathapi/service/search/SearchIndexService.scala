/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.{Elastic4sClient, SearchApiClient}
import no.ndla.learningpathapi.model.domain.Language._
import no.ndla.learningpathapi.model.domain.{ElasticIndexingException, LearningPath, ReindexResult}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import org.json4s.native.Serialization._

import scala.util.{Failure, Success, Try}

trait SearchIndexService {
  this: Elastic4sClient
    with SearchConverterServiceComponent
    with LearningPathRepositoryComponent
    with SearchApiClient =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        createIndexWithGeneratedName().flatMap(indexName => {
          val operations = for {
            numIndexed <- sendToElastic(indexName)
            aliasTarget <- aliasTarget
            _ <- updateAliasTarget(aliasTarget, indexName)
            _ <- deleteIndexWithName(aliasTarget)
          } yield numIndexed

          operations match {
            case Failure(f) => {
              deleteIndexWithName(Some(indexName))
              Failure(f)
            }
            case Success(totalIndexed) => {
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
            }
          }
        })
      }
    }

    def indexDocument(learningPath: LearningPath): Try[LearningPath] = {
      for {
        _ <- aliasTarget.map {
          case Some(index) => Success(index)
          case None =>
            createIndexWithGeneratedName().map(newIndex => updateAliasTarget(None, newIndex))
        }
        indexed <- {
          val source = write(searchConverterService.asSearchableLearningpath(learningPath))

          val response = e4sClient.execute {
            indexInto(LearningpathApiProperties.SearchIndex / LearningpathApiProperties.SearchDocument)
              .doc(source)
              .id(learningPath.id.get.toString)
          }
          response match {
            case Success(_)  => Success(learningPath)
            case Failure(ex) => Failure(ex)
          }
        }
        _ <- searchApiClient.indexLearningPathDocument(learningPath)
      } yield indexed
    }

    def deleteDocument(learningPath: LearningPath): Try[_] = {
      learningPath.id
        .map(id => {
          for {
            _ <- searchIndexService.aliasTarget.map {
              case Some(index) => Success(index)
              case None =>
                createIndexWithGeneratedName().map(newIndex => updateAliasTarget(None, newIndex))
            }
            deleted <- {
              e4sClient.execute {
                delete(id.toString)
                  .from(LearningpathApiProperties.SearchIndex / LearningpathApiProperties.SearchDocument)
              }
            }
            _ <- searchApiClient.deleteLearningPathDocument(id)
          } yield deleted
        })
        .getOrElse(Success(learningPath))
    }

    def createIndexWithGeneratedName(): Try[String] = {
      createIndexWithName(LearningpathApiProperties.SearchIndex + "_" + getTimestamp)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          createIndex(indexName)
            .mappings(buildMapping)
            .indexSetting("max_result_window", LearningpathApiProperties.ElasticSearchIndexMaxResultWindow)
        }

        response match {
          case Success(_)  => Success(indexName)
          case Failure(ex) => Failure(ex)
        }

      }
    }

    private def sendToElastic(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk =
            searchIndexService.indexLearningPaths(learningPathRepository.learningPathsWithIdBetween(range._1, range._2),
                                                  indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f)   => return Failure(f)
          }
        })
        numIndexed
      })
    }

    private def getRanges: Try[List[(Long, Long)]] = {
      Try {
        val (minId, maxId) = learningPathRepository.minMaxId
        Seq
          .range(minId, maxId)
          .grouped(LearningpathApiProperties.IndexBulkSize)
          .map(group => (group.head, group.last + 1))
          .toList
      }
    }

    private def indexLearningPaths(learningPaths: List[LearningPath], indexName: String): Try[Int] = {
      if (learningPaths.isEmpty) {
        Success(0)
      } else {
        val response = e4sClient.execute {
          bulk(learningPaths.map(lp => {
            val source =
              write(searchConverterService.asSearchableLearningpath(lp))
            indexInto(indexName / LearningpathApiProperties.SearchDocument)
              .doc(source)
              .id(lp.id.get.toString)
          }))
        }

        response match {
          case Success(RequestSuccess(_, _, _, result)) if !result.errors =>
            logger.info(s"Indexed ${learningPaths.size} documents")
            Success(learningPaths.size)
          case Success(RequestSuccess(_, _, _, result)) =>
            val failed = result.items.collect {
              case item if item.error.isDefined =>
                s"'${item.id}: ${item.error.get.reason}'"
            }

            logger.error(s"Failed to index ${failed.length} items: ${failed.mkString(", ")}")
            Failure(ElasticIndexingException(s"Failed to index ${failed.size}/${learningPaths.size} learningpaths"))
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) => {
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            e4sClient.execute {
              deleteIndex(indexName)
            }
          }
        }
      }
    }

    private def aliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute {
        getAliases(Nil, List(LearningpathApiProperties.SearchIndex))
      }

      response match {
        case Success(results) =>
          Success(results.result.mappings.headOption.map(t => t._1.name))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        oldIndexName match {
          case None =>
            e4sClient.execute(addAlias(LearningpathApiProperties.SearchIndex).on(newIndexName))
          case Some(oldIndex) =>
            e4sClient.execute {
              removeAlias(LearningpathApiProperties.SearchIndex).on(oldIndex)
              addAlias(LearningpathApiProperties.SearchIndex).on(newIndexName)
            }
        }
      }
    }

    private def buildMapping = {
      mapping(LearningpathApiProperties.SearchDocument).fields(
        intField("id"),
        languageSupportedField("titles", keepRaw = true),
        languageSupportedField("descriptions"),
        textField("coverPhotoUrl"),
        intField("duration"),
        textField("status"),
        textField("verificationStatus"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        languageSupportedField("tags", keepRaw = true),
        textField("author"),
        nestedField("learningsteps").fields(
          textField("stepType"),
          languageSupportedField("titles"),
          languageSupportedField("descriptions")
        ),
        objectField("copyright").fields(
          objectField("license").fields(
            textField("license"),
            textField("description"),
            textField("url")
          ),
          nestedField("contributors").fields(
            textField("type"),
            textField("name")
          )
        ),
        intField("isBasedOn")
      )
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = NestedFieldDefinition(fieldName).fields(
        keepRaw match {
          case true =>
            languageAnalyzers.map(
              langAnalyzer =>
                textField(langAnalyzer.lang)
                  .fielddata(true)
                  .analyzer(langAnalyzer.analyzer)
                  .fields(keywordField("raw")))
          case false =>
            languageAnalyzers.map(
              langAnalyzer =>
                textField(langAnalyzer.lang)
                  .fielddata(true)
                  .analyzer(langAnalyzer.analyzer))
        }
      )
      languageSupportedField

    }

    def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_)                          => Success(false)
        case Failure(ex)                         => Failure(ex)
      }
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss")
        .format(Calendar.getInstance.getTime)
    }
  }

}
