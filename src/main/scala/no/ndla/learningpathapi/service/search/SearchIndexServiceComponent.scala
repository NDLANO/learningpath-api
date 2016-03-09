package no.ndla.learningpathapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{DateType, IntegerType, NestedType, StringType}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.ElasticClientComponent
import no.ndla.learningpathapi.model.LearningPath
import no.ndla.learningpathapi.service.ConverterServiceComponent
import no.ndla.learningpathapi.service.search.model.SearchConverter
import org.json4s.native.Serialization._


trait SearchIndexServiceComponent {
  this: ElasticClientComponent with ConverterServiceComponent =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats

    def indexLearningPaths(learningPaths: List[LearningPath], indexName: String): Int = {
      elasticClient.execute {
        bulk(learningPaths.map(learningPath => {
          index into indexName -> LearningpathApiProperties.SearchDocument source write(SearchConverter.asSearchableLearningPath(learningPath)) id learningPath.id.get
        }))
      }.await

      logger.info(s"Indexed ${learningPaths.size} documents")
      learningPaths.size
    }

    def indexLearningPath(learningPath: LearningPath): Unit = {
      aliasTarget.foreach(indexName => {
        elasticClient.execute {
          index into indexName -> LearningpathApiProperties.SearchDocument source write(converterService.asApiLearningpath(learningPath)) id learningPath.id.get
        }.await
      })
    }

    def deleteLearningPath(learningPath: LearningPath): Unit = {
      aliasTarget.foreach(indexName => {
        elasticClient.execute{
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

    private def createElasticIndex(indexName: String) = {
      elasticClient.execute {
        createIndex(indexName) mappings(
          LearningpathApiProperties.SearchDocument as(
            "id" typed IntegerType,
            "title" typed NestedType as(
              "title" typed StringType fields("raw" typed StringType index "not_analyzed"),
              "language" typed StringType index "not_analyzed"),
            "description" typed NestedType as(
              "description" typed StringType,
              "language" typed StringType index "not_analyzed"),
            "metaUrl" typed StringType index "not_analyzed",
            "learningsteps" typed NestedType as(
              "id" typed IntegerType,
              "seqNo" typed IntegerType,
              "title" typed NestedType as(
                "title" typed StringType,
                "language" typed StringType index "not_analyzed"),
              "description" typed NestedType as(
                "description" typed StringType ,
                "language" typed StringType index "not_analyzed"),
              "embedContent" typed NestedType as(
                "url" typed StringType ,
                "html" typed StringType index "not_analyzed",
                "language" typed StringType index "not_analyzed"),
              "type" typed StringType,
              "license" typed StringType,
              "metaUrl" typed StringType),
            "learningstepUrl" typed StringType index "not_analyzed",
            "coverPhotoUrl" typed StringType index "not_analyzed",
            "duration" typed IntegerType,
            "status" typed StringType index "not_analyzed",
            "verificationStatus" typed StringType index "not_analyzed",
            "lastUpdated" typed DateType,
            "tags" typed NestedType as(
              "tag" typed StringType,
              "language" typed StringType index "not_analyzed"),
            "author" typed NestedType as(
              "type" typed StringType index "not_analyzed",
              "name" typed StringType)
            )
          )
      }.await
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }
}
