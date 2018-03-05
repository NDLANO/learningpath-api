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

import com.sksamuel.elastic4s.alias.AliasActionDefinition
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.Elastic4sClient
import no.ndla.learningpathapi.model.domain.Language._
import no.ndla.learningpathapi.model.domain.{ElasticIndexingException, LearningPath, ReindexResult}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import org.json4s.native.Serialization._

import scala.util.{Failure, Success, Try}


trait SearchIndexServiceComponent {
  this: Elastic4sClient with SearchConverterServiceComponent with LearningPathRepositoryComponent =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats
    val searchIndex: String = LearningpathApiProperties.SearchIndex
    val searchDocument: String = LearningpathApiProperties.SearchDocument

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        createIndexWithGeneratedName().flatMap(indexName => {
          val operations = for {
            numIndexed <- sendToElastic(indexName)
            aliasTarget <- aliasTarget
            updatedTarget <- updateAliasTarget(aliasTarget, indexName)
            deleted <- deleteIndexWithName(aliasTarget)
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
          case None => createIndexWithGeneratedName().map(newIndex => updateAliasTarget(None, newIndex))
        }
        indexed <- {
          val source = write(searchConverterService.asSearchableLearningpath(learningPath))

          val response = e4sClient.execute{
            indexInto(searchIndex / searchDocument)
              .doc(source)
              .id(learningPath.id.get.toString)
          }
          response match {
            case Success(_) => Success(learningPath)
            case Failure(ex) => Failure(ex)
          }
        }
      } yield indexed
    }

    def deleteDocument(learningPath: LearningPath): Try[_] = {
      for {
        _ <- searchIndexService.aliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndexWithGeneratedName().map(newIndex => updateAliasTarget(None, newIndex))
        }
        deleted <- {
          e4sClient.execute{
            delete(s"${learningPath.id.get}")
              .from(searchIndex / searchDocument)
          }
        }
      } yield deleted
    }

    def createIndexWithGeneratedName(): Try[String] = {
      createIndexWithName(searchIndex + "_" + getTimestamp)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute{
          createIndex(indexName)
            .mappings(buildMapping)
            .indexSetting("max_result_window", LearningpathApiProperties.ElasticSearchIndexMaxResultWindow)
        }

        response match {
          case Success(_) => Success(indexName)
          case Failure(ex) => Failure(ex)
        }

      }
    }

    private def sendToElastic(indexName: String): Try[Int] = {
      getRanges.map(ranges => {
        ranges.map(range => {
          searchIndexService.indexLearningPaths(learningPathRepository.learningPathsWithIdBetween(range._1, range._2), indexName)
        }).map({
          case Success(s) => s
          case Failure(ex) => return Failure(ex)
        }).sum
      })
    }

    private def getRanges:Try[List[(Long,Long)]] = {
      Try{
        val (minId, maxId) = learningPathRepository.minMaxId
        Seq.range(minId, maxId).grouped(LearningpathApiProperties.IndexBulkSize).map(group => (group.head, group.last + 1)).toList
      }
    }

    private def indexLearningPaths(learningPaths: List[LearningPath], indexName: String): Try[Int] = {
      if (learningPaths.isEmpty) {
        Success(0)
      } else {
        val response = e4sClient.execute {
          bulk(learningPaths.map(lp => {
            val source = write(searchConverterService.asSearchableLearningpath(lp))
            indexInto(indexName / searchDocument).doc(source).id(lp.id.get.toString)
          }))
        }

        response match {
          case Success(RequestSuccess(_, _, _, result)) if !result.errors =>
            logger.info(s"Indexed ${learningPaths.size} documents")
            Success(learningPaths.size)
          case Success(RequestSuccess(_, _, _, result)) =>
            val failed = result.items.collect {
              case item if item.error.isDefined => s"'${item.id}: ${item.error.get.reason}'"
            }

            logger.error(s"Failed to index ${failed.length} items: ${failed.mkString(", ")}")
            Failure(ElasticIndexingException(s"Failed to index ${failed.size}/${learningPaths.size} learningpaths"))
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success()
        case Some(indexName) => {
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            e4sClient.execute{
              deleteIndex(indexName)
            }
          }
        }
      }
    }

    private def aliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute{
        getAliases(Nil, List(searchIndex))
      }

      response match {
        case Success(results) => Success(results.result.mappings.headOption.map(t => t._1.name))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val actions = oldIndexName match {
          case None =>
            List[AliasActionDefinition](addAlias(searchIndex).on(newIndexName))
          case Some(oldIndex) =>
            List[AliasActionDefinition](
              removeAlias(searchIndex).on(oldIndex),
              addAlias(searchIndex).on(newIndexName)
            )
        }

        e4sClient.execute(aliases(actions)) match {
          case Success(_) =>
            logger.info("Alias target updated successfully, deleting other indexes.")
            cleanupIndexes()
          case Failure(ex) =>
            logger.error("Could not update alias target.")
            Failure(ex)
        }

      }
    }

    /**
      * Deletes every index that is not in use by this indexService.
      * Only indexes starting with indexName are deleted.
      * @param indexName Start of index names that is deleted if not aliased.
      * @return Name of aliasTarget.
      */
    def cleanupIndexes(indexName: String = searchIndex): Try[String] = {
      e4sClient.execute(getAliases()) match {
        case Success(s) =>
          val indexes = s.result.mappings.filter(_._1.name.startsWith(indexName))
          val unreferencedIndexes = indexes.filter(_._2.isEmpty).map(_._1.name).toList
          val (aliasTarget, aliasIndexesToDelete) = indexes.filter(_._2.nonEmpty).map(_._1.name) match {
            case head :: tail =>
              (head, tail)
            case _ =>
              logger.warn("No alias found, when attempting to clean up indexes.")
              ("", List.empty)
          }

          val toDelete = unreferencedIndexes ++ aliasIndexesToDelete

          if (toDelete.isEmpty){
            logger.info("No indexes to be deleted.")
            Success(aliasTarget)
          } else {
            e4sClient.execute {
              deleteIndex(toDelete)
            } match {
              case Success(_) =>
                logger.info(s"Successfully deleted unreferenced and redundant indexes.")
                Success(aliasTarget)
              case Failure(ex) =>
                logger.error("Could not delete unreferenced and redundant indexes.")
                Failure(ex)
            }
          }
        case Failure(ex) =>
          logger.warn("Could not fetch aliases after updating alias.")
          Failure(ex)
      }

    }

    private def buildMapping = {
      mapping(searchDocument).fields(
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
          case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer).fields(keywordField("raw")))
          case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer))
        }
      )
      languageSupportedField


    }

    private def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_) => Success(false)
        case Failure(ex) => Failure(ex)
      }
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
