package no.ndla.learningpathapi.integration

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.mappings.FieldType.{DateType, IntegerType, NestedType, StringType}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.business.LearningPathIndex
import no.ndla.learningpathapi.model.LearningPath
import no.ndla.learningpathapi.service.ModelConverters
import org.elasticsearch.common.settings.ImmutableSettings
import org.json4s.native.Serialization._

class ElasticLearningPathIndex(clusterName: String, clusterHost: String, clusterPort: String) extends LearningPathIndex with LazyLogging {
  implicit val formats = org.json4s.DefaultFormats

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build()
  val client = ElasticClient.remote(settings, ElasticsearchClientUri(s"elasticsearch://$clusterHost:$clusterPort"))


  override def indexDocuments(learningPaths: List[LearningPath], indexName: String): Int = {
    client.execute {
      bulk(learningPaths.map(learningPath => {
        index into indexName -> LearningpathApiProperties.SearchDocument source write(ModelConverters.asApiLearningpath(learningPath)) id learningPath.id.get
      }))
    }.await

    logger.info(s"Indexed ${learningPaths.size} documents")
    learningPaths.size
  }

  override def aliasTarget: Option[String] = {
    val res = client.execute {
      get alias LearningpathApiProperties.SearchIndex
    }.await
    val aliases = res.getAliases.keysIt()
    aliases.hasNext match {
      case true => Some(aliases.next())
      case false => None
    }
  }

  override def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Unit = {
    val existsDefinition = client.execute {
      index exists newIndexName
    }.await

    if (existsDefinition.isExists) {
      client.execute {
        oldIndexName.foreach(oldIndexName => {
          remove alias LearningpathApiProperties.SearchIndex on oldIndexName

        })
        add alias LearningpathApiProperties.SearchIndex on newIndexName
      }.await
    } else {
      throw new IllegalArgumentException(s"No such index: $newIndexName")
    }
  }

  override def delete(indexName: String): Unit = {
    val existsDefinition = client.execute {
      index exists indexName
    }.await

    if (existsDefinition.isExists) {
      client.execute {
        deleteIndex(indexName)
      }.await
    } else {
      throw new IllegalArgumentException(s"No such index: $indexName")
    }
  }


  override def create(): String = {
    val indexName = LearningpathApiProperties.SearchIndex + "_" + getTimestamp

    val existsDefinition = client.execute {
      index exists indexName.toString
    }.await

    if (!existsDefinition.isExists) {
      createElasticIndex(indexName)
    }

    indexName
  }

  def createElasticIndex(indexName: String) = {
    client.execute {
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
            "embedUrl" typed NestedType as(
              "url" typed StringType ,
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

  def getTimestamp: String = {
    new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
  }
}
