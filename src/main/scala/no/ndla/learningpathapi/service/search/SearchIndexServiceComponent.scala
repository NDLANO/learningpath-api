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
import com.sksamuel.elastic4s.mappings.FieldType.{DateType, IntegerType, NestedType, StringType}
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Delete, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.ElasticClientComponent
import no.ndla.learningpathapi.model.domain.Language._
import no.ndla.learningpathapi.model.domain.LearningPath
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization._


trait SearchIndexServiceComponent {
  this: ElasticClientComponent with SearchConverterServiceComponent =>
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

    def indexLearningPaths(learningPaths: List[LearningPath], indexName: String): Int = {
      val bulkBuilder = new Bulk.Builder()
      learningPaths.foreach(lp => {
        val source = write(searchConverterService.asSearchableLearningpath(lp))
        bulkBuilder.addAction(new Index.Builder(source).index(indexName).`type`(LearningpathApiProperties.SearchDocument).id(s"${lp.id.get}").build())
      })

      val response = jestClient.execute(bulkBuilder.build())
      if (!response.isSucceeded) {
        throw new ElasticsearchException(s"Unable to index documents to ${LearningpathApiProperties.SearchIndex}. ErrorMessage: {}", response.getErrorMessage)
      }
      logger.info(s"Indexed ${learningPaths.size} documents")
      learningPaths.size
    }

    def indexLearningPath(learningPath: LearningPath): Unit = {

      aliasTarget.foreach(indexName => {
        val source = write(searchConverterService.asSearchableLearningpath(learningPath))
        val indexReq = new Index.Builder(source).index(indexName).`type`(LearningpathApiProperties.SearchDocument).id(s"${learningPath.id.get}").build()
        val response = jestClient.execute(indexReq)
        if (!response.isSucceeded) {
          throw new ElasticsearchException(s"Unable to index document with id ${learningPath.id} to ${LearningpathApiProperties.SearchIndex}. ErrorMessage: {}", response.getErrorMessage)
        }
      })
    }

    def deleteLearningPath(learningPath: LearningPath): Unit = {
      aliasTarget.foreach(indexName => {
        val deleteReq = new Delete.Builder(s"${learningPath.id.get}").index(indexName).`type`(LearningpathApiProperties.SearchDocument).build()
        val response = jestClient.execute(deleteReq)
        if (!response.isSucceeded) {
          throw new ElasticsearchException(s"Unable to delete document with id ${learningPath.id} from ${LearningpathApiProperties.SearchIndex}. ErrorMessage: {}", response.getErrorMessage)
        }
      })
    }

    def createNewIndex(): String = {
      val indexName = LearningpathApiProperties.SearchIndex + "_" + getTimestamp
      if (!indexExists(indexName)) {
        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).build())
        createIndexResponse.isSucceeded match {
          case false => throw new ElasticsearchException(s"Unable to create index $indexName. ErrorMessage: {}", createIndexResponse.getErrorMessage)
          case true => createMapping(indexName)
        }
      }
      indexName
    }

    def createMapping(indexName: String) = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, LearningpathApiProperties.SearchDocument, buildMapping()).build())
      if (!mappingResponse.isSucceeded) {
        throw new ElasticsearchException(s"Unable to create mapping for index $indexName. ErrorMessage: {}", mappingResponse.getErrorMessage)
      }
    }

    def removeIndex(indexName: String) = {
      if (indexExists(indexName)) {
        val response = jestClient.execute(new DeleteIndex.Builder(indexName).build())
        if (!response.isSucceeded) {
          throw new ElasticsearchException(s"Unable to delete index $indexName. ErrorMessage: {}", response.getErrorMessage)
        }
      } else {
        throw new IllegalArgumentException(s"No such index: $indexName")
      }
    }

    def aliasTarget: Option[String] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${LearningpathApiProperties.SearchIndex}").build()
      val result = jestClient.execute(getAliasRequest)
      result.isSucceeded match {
        case false => None
        case true => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Some(aliasIterator.next().getKey)
            case false => None
          }
        }
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String) = {
      if (indexExists(newIndexName)) {
        val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, LearningpathApiProperties.SearchIndex).build()
        val modifyAliasRequest = oldIndexName match {
          case None => new ModifyAliases.Builder(addAliasDefinition).build()
          case Some(oldIndex) => {
            new ModifyAliases.Builder(
              new RemoveAliasMapping.Builder(oldIndex, LearningpathApiProperties.SearchIndex).build()
            ).addAlias(addAliasDefinition).build()
          }
        }

        val response = jestClient.execute(modifyAliasRequest)
        if (!response.isSucceeded) {
          logger.error(response.getErrorMessage)
          throw new ElasticsearchException(s"Unable to modify alias ${LearningpathApiProperties.SearchIndex} -> $oldIndexName to ${LearningpathApiProperties.SearchIndex} -> $newIndexName. ErrorMessage: {}", response.getErrorMessage)
        }
      } else {
        throw new IllegalArgumentException(s"No such index: $newIndexName")
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
          )
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

    def indexExists(indexName: String): Boolean = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()).isSucceeded
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
