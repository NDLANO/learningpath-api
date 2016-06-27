package no.ndla.learningpathapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType.{DateType, IntegerType, NestedType, StringType}
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.ElasticClientComponent
import no.ndla.learningpathapi.model.domain.LearningPath
import no.ndla.mapping.ISO639Mapping._
import org.json4s.jackson.Serialization._


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
      elasticClient.execute {
        bulk(learningPaths.map(learningPath => {
          index into indexName -> LearningpathApiProperties.SearchDocument source write(searchConverterService.asSearchableLearningpath(learningPath)) id learningPath.id.get
        }))
      }.await

      logger.info(s"Indexed ${learningPaths.size} documents")
      learningPaths.size
    }

    def indexLearningPath(learningPath: LearningPath): Unit = {
      aliasTarget.foreach(indexName => {
        elasticClient.execute {
          index into indexName -> LearningpathApiProperties.SearchDocument source write(searchConverterService.asSearchableLearningpath(learningPath)) id learningPath.id.get
        }.await
      })
    }

    def deleteLearningPath(learningPath: LearningPath): Unit = {
      aliasTarget.foreach(indexName => {
        elasticClient.execute {
          delete id learningPath.id.get from indexName / LearningpathApiProperties.SearchDocument
        }.await
      })
    }

    def createNewIndex(): String = {
      val indexName = LearningpathApiProperties.SearchIndex + "_" + getTimestamp

      val existsDefinition = elasticClient.execute {
        index exists indexName.toString
      }.await

      if (!existsDefinition.isExists) {
        createElasticIndex(indexName)
      }
      indexName
    }

    def removeIndex(indexName: String): Unit = {
      val existsDefinition = elasticClient.execute {
        index exists indexName
      }.await

      if (existsDefinition.isExists) {
        elasticClient.execute {
          deleteIndex(indexName)
        }.await
      } else {
        throw new IllegalArgumentException(s"No such index: $indexName")
      }
    }

    def aliasTarget: Option[String] = {
      val res = elasticClient.execute {
        get alias LearningpathApiProperties.SearchIndex
      }.await
      val aliases = res.getAliases.keysIt()
      aliases.hasNext match {
        case true => Some(aliases.next())
        case false => None
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Unit = {
      val existsDefinition = elasticClient.execute {
        index exists newIndexName
      }.await

      if (existsDefinition.isExists) {
        elasticClient.execute {
          oldIndexName.foreach(oldIndexName => {
            remove alias LearningpathApiProperties.SearchIndex on oldIndexName

          })
          add alias LearningpathApiProperties.SearchIndex on newIndexName
        }.await
      } else {
        throw new IllegalArgumentException(s"No such index: $newIndexName")
      }
    }

    private def languageSupportedField(fieldName: String, keepRaw:Boolean = false) = {
      if(keepRaw) {
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



    private def createElasticIndex(indexName: String) = {
      elasticClient.execute {
        createIndex(indexName) mappings (
          LearningpathApiProperties.SearchDocument as(
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
          )
        )
      }.await
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
