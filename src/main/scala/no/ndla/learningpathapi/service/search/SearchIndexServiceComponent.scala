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

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Delete, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.ElasticClientComponent
import no.ndla.learningpathapi.model.domain.Language._
import no.ndla.learningpathapi.model.domain.{LearningPath, NdlaSearchException, ReindexResult}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization._

import scala.util.{Failure, Success, Try}


trait SearchIndexServiceComponent {
  this: ElasticClientComponent with SearchConverterServiceComponent with LearningPathRepositoryComponent =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {
    val langToAnalyzer = Map(
      NORWEGIAN_BOKMAL -> NorwegianLanguageAnalyzer,
      NORWEGIAN_NYNORSK -> NorwegianLanguageAnalyzer,
      ENGLISH -> EnglishLanguageAnalyzer,
      FRENCH -> FrenchLanguageAnalyzer,
      GERMAN -> GermanLanguageAnalyzer,
      SPANISH -> SpanishLanguageAnalyzer,
      SAMI -> StandardAnalyzer,
      CHINESE -> ChineseLanguageAnalyzer,
      UNKNOWN -> StandardAnalyzer
    )

    implicit val formats = org.json4s.DefaultFormats

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        createIndex().flatMap(indexName => {
          val operations = for {
            numIndexed <- sendToElastic(indexName)
            aliasTarget <- aliasTarget
            updatedTarget <- updateAliasTarget(aliasTarget, indexName)
            deleted <- removeIndex(aliasTarget)
          } yield numIndexed

          operations match {
            case Failure(f) => {
              removeIndex(Some(indexName))
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
          case None => createIndex().map(newIndex => updateAliasTarget(None, newIndex))
        }
        indexed <- {
          val indexRequest = new Index.Builder(write(searchConverterService.asSearchableLearningpath(learningPath)))
            .index(LearningpathApiProperties.SearchIndex)
            .`type`(LearningpathApiProperties.SearchDocument)
            .id(learningPath.id.get.toString).build

          jestClient.execute(indexRequest).map(_ => learningPath)
        }
      } yield indexed
    }

    def deleteDocument(learningPath: LearningPath): Try[_] = {
      for {
        _ <- searchIndexService.aliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndex().map(newIndex => updateAliasTarget(None, newIndex))
        }
        deleted <- {
          jestClient.execute(
            new Delete.Builder(s"${learningPath.id.get}").index(LearningpathApiProperties.SearchIndex).`type`(LearningpathApiProperties.SearchDocument).build()
          )
        }
      } yield deleted
    }

    def createIndex(): Try[String] = {
      val indexName = LearningpathApiProperties.SearchIndex + "_" + getTimestamp
      if (indexExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).build())
        createIndexResponse.map(_ => createMapping(indexName)).map(_ => indexName)
      }
    }

    private def sendToElastic(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = searchIndexService.indexLearningPaths(learningPathRepository.learningPathsWithIdBetween(range._1, range._2), indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f) => return Failure(f)
          }
        })
        numIndexed
      })
    }

    private def getRanges:Try[List[(Long,Long)]] = {
      Try{
        val (minId, maxId) = learningPathRepository.minMaxId
        Seq.range(minId, maxId).grouped(LearningpathApiProperties.IndexBulkSize).map(group => (group.head, group.last + 1)).toList
      }
    }

    private def indexLearningPaths(learningPaths: List[LearningPath], indexName: String): Try[Int] = {
      val bulkBuilder = new Bulk.Builder()
      learningPaths.foreach(lp => {
        val source = write(searchConverterService.asSearchableLearningpath(lp))
        bulkBuilder.addAction(new Index.Builder(source).index(indexName).`type`(LearningpathApiProperties.SearchDocument).id(s"${lp.id.get}").build())
      })

      val response = jestClient.execute(bulkBuilder.build())
      response.map(_ => {
        logger.info(s"Indexed ${learningPaths.size} documents")
        learningPaths.size
      })
    }

    private def createMapping(indexName: String): Try[String] = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, LearningpathApiProperties.SearchDocument, buildMapping()).build())
      mappingResponse.map(_ => indexName)
    }


    private def removeIndex(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success()
        case Some(indexName) => {
          if (!indexExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            jestClient.execute(new DeleteIndex.Builder(indexName).build())
          }
        }
      }
    }

    private def aliasTarget: Try[Option[String]] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${LearningpathApiProperties.SearchIndex}").build()
      jestClient.execute(getAliasRequest) match {
        case Success(result) => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Success(Some(aliasIterator.next().getKey))
            case false => Success(None)
          }
        }
        case Failure(_: NdlaSearchException) => Success(None)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, LearningpathApiProperties.SearchIndex).build()
        val modifyAliasRequest = oldIndexName match {
          case None => new ModifyAliases.Builder(addAliasDefinition).build()
          case Some(oldIndex) => {
            new ModifyAliases.Builder(
              new RemoveAliasMapping.Builder(oldIndex, LearningpathApiProperties.SearchIndex).build()
            ).addAlias(addAliasDefinition).build()
          }
        }

        jestClient.execute(modifyAliasRequest)
      }
    }

    private def buildMapping() = {
      mapping(LearningpathApiProperties.SearchDocument).fields(
        "id" typed IntegerType,
        languageSupportedField("titles", keepRaw = true),
        languageSupportedField("descriptions"),
        "coverPhotoUrl" typed StringType index "not_analyzed",
        "duration" typed IntegerType,
        "status" typed StringType index "not_analyzed",
        "verificationStatus" typed StringType index "not_analyzed",
        "lastUpdated" typed DateType,
        languageSupportedField("tags", keepRaw = true),
        "author" typed StringType,
        "learningsteps" typed NestedType as(
          "stepType" typed StringType index "not_analyzed",
          languageSupportedField("titles"),
          languageSupportedField("descriptions")
          ),
        "copyright" typed ObjectType as(
          "license" typed ObjectType as(
              "license" typed StringType index "not_analyzed",
              "description" typed StringType index "not_analyzed",
              "url" typed StringType index "not_analyzed"
            ),
          "contributors" typed NestedType as(
            "type" typed StringType index "not_analyzed",
            "name" typed StringType index "not_analyzed"
            )
          ),
        "isBasedOn" typed BooleanType index "not_analyzed"
      ).buildWithName.string()
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      if (keepRaw) {
        new NestedFieldDefinition(fieldName).as(
          NORWEGIAN_BOKMAL typed StringType analyzer langToAnalyzer(NORWEGIAN_BOKMAL) fields ("raw" typed StringType index "not_analyzed"),
          NORWEGIAN_NYNORSK typed StringType analyzer langToAnalyzer(NORWEGIAN_NYNORSK) fields ("raw" typed StringType index "not_analyzed"),
          ENGLISH typed StringType analyzer langToAnalyzer(ENGLISH) fields ("raw" typed StringType index "not_analyzed"),
          FRENCH typed StringType analyzer langToAnalyzer(FRENCH) fields ("raw" typed StringType index "not_analyzed"),
          GERMAN typed StringType analyzer langToAnalyzer(GERMAN) fields ("raw" typed StringType index "not_analyzed"),
          SPANISH typed StringType analyzer langToAnalyzer(SPANISH) fields ("raw" typed StringType index "not_analyzed"),
          SAMI typed StringType analyzer langToAnalyzer(SAMI) fields ("raw" typed StringType index "not_analyzed"),
          CHINESE typed StringType analyzer langToAnalyzer(CHINESE) fields ("raw" typed StringType index "not_analyzed"),
          UNKNOWN typed StringType analyzer langToAnalyzer(UNKNOWN) fields ("raw" typed StringType index "not_analyzed")
        )
      } else {
        new NestedFieldDefinition(fieldName).as(
          NORWEGIAN_BOKMAL typed StringType analyzer langToAnalyzer(NORWEGIAN_BOKMAL),
          NORWEGIAN_NYNORSK typed StringType analyzer langToAnalyzer(NORWEGIAN_NYNORSK),
          ENGLISH typed StringType analyzer langToAnalyzer(ENGLISH),
          FRENCH typed StringType analyzer langToAnalyzer(FRENCH),
          GERMAN typed StringType analyzer langToAnalyzer(GERMAN),
          SPANISH typed StringType analyzer langToAnalyzer(SPANISH),
          SAMI typed StringType analyzer langToAnalyzer(SAMI),
          CHINESE typed StringType analyzer langToAnalyzer(CHINESE),
          UNKNOWN typed StringType analyzer langToAnalyzer(UNKNOWN)
        )
      }
    }

    private def indexExists(indexName: String): Try[Boolean] = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()) match {
        case Success(_) => Success(true)
        case Failure(_: ElasticsearchException) => Success(false)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
