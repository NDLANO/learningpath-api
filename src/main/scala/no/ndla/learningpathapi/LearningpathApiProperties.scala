/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model.domain.Language
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets

import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Properties, Success, Try}
object LearningpathApiProperties extends LazyLogging {

  var LearningpathApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  val SecretsFile = "learningpath_api.secrets"
  val NdlaEnvironment = Properties.envOrElse("NDLA_ENVIRONMENT", "local")

  lazy val ApplicationPort = 80
  lazy val ContactEmail = "christergundersen@ndla.no"

  lazy val MetaUserName = get(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword = get(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource = get(PropertyKeys.MetaResourceKey)
  lazy val MetaServer = get(PropertyKeys.MetaServerKey)
  lazy val MetaPort = getInt(PropertyKeys.MetaPortKey)
  lazy val MetaSchema = get(PropertyKeys.MetaSchemaKey)

  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  lazy val SearchServer = getOrElse("SEARCH_SERVER", "http://search-learningpath-api.ndla-local")
  lazy val SearchRegion = getOrElse("SEARCH_REGION", "eu-central-1")
  lazy val RunWithSignedSearchRequests = getOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  val SearchIndex = "learningpaths"
  val SearchDocument = "learningpath"
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 1000

  lazy val MigrationHost = get("MIGRATION_HOST")
  lazy val MigrationUser = get("MIGRATION_USER")
  lazy val MigrationPassword = get("MIGRATION_PASSWORD")

  val AuthHost = "auth.ndla-local"
  val ImageApiHost = "image-api.ndla-local"
  val MappingHost = "mapping-api.ndla-local"
  val DefaultLanguage = Language.NORWEGIAN_BOKMAL
  val UsernameHeader = "X-Consumer-Username"

  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  val BasicHtmlTags = List("b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em",
    "i", "li", "ol", "p", "pre", "q", "small", "strike", "strong",
    "sub", "sup", "u", "ul")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  def setProperties(properties: Map[String, Option[String]]) = {
    val missingProperties = properties.filter(_._2.isEmpty).keys
    missingProperties.isEmpty match {
      case true => Success(properties.foreach(prop => LearningpathApiProps.put(prop._1, prop._2)))
      case false => Failure(new RuntimeException(s"Missing required properties: ${missingProperties.mkString(", ")}"))
    }
  }

  private def getOrElse(envKey: String, defaultValue: String) = {
    LearningpathApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => defaultValue
    }
  }

  private def get(envKey: String): String = {
    LearningpathApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  private def getInt(envKey: String): Integer = {
    get(envKey).toInt
  }

  private def getBoolean(envKey: String): Boolean = {
    get(envKey).toBoolean
  }
}

object PropertiesLoader extends LazyLogging {
  val EnvironmentFile = "/learningpath-api.env"

  private def readPropertyFile() = {
    Try(Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().withFilter(line => line.matches("^\\w+$")).map(key => key -> Properties.envOrNone(key)).toMap)
  }

  def load() = {
    val verification = for {
      file <- readPropertyFile()
      secrets <- readSecrets(LearningpathApiProperties.SecretsFile)
      didSetProperties <- LearningpathApiProperties.setProperties(file ++ secrets)
    } yield didSetProperties

    if(verification.isFailure){
      logger.error("Unable to load properties", verification.failed.get)
      System.exit(1)
    }
  }
}
