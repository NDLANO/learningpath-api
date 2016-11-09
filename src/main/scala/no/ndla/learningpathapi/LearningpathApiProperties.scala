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

import scala.util.Properties._
object LearningpathApiProperties extends LazyLogging {

  val SecretsFile = "learningpath_api.secrets"

  val ApplicationPort = 80
  val ContactEmail = "christergundersen@ndla.no"

  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  val SearchIndex = "learningpaths"
  val SearchDocument = "learningpath"
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 1000

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

  val secrets = readSecrets(SecretsFile).getOrElse(throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile"))
  val Environment = envOrElse("NDLA_ENVIRONMENT", "local")

  val MetaUserName = secretOrEnvOrFail(PropertyKeys.MetaUserNameKey)
  val MetaPassword = secretOrEnvOrFail(PropertyKeys.MetaPasswordKey)
  val MetaResource = secretOrEnvOrFail(PropertyKeys.MetaResourceKey)
  val MetaServer = secretOrEnvOrFail(PropertyKeys.MetaServerKey)
  val MetaPort = secretOrEnvOrFail(PropertyKeys.MetaPortKey).toInt
  val MetaSchema = secretOrEnvOrFail(PropertyKeys.MetaSchemaKey)

  val SearchServer = envOrElse("SEARCH_SERVER", "http://search-learningpath-api.ndla-local")
  val SearchRegion = envOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = envOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  val MigrationHost = envOrFail("MIGRATION_HOST")
  val MigrationUser = envOrFail("MIGRATION_USER")
  val MigrationPassword = envOrFail("MIGRATION_PASSWORD")

  def envOrFail(envVariable: String): String = {
    envOrNone(envVariable) match {
      case Some(x) => x
      case None => {
        throw new RuntimeException(s"Unable to load property $envVariable")
      }
    }
  }

  def secretOrEnvOrFail(key: String): String = {
    secrets.get(key).flatten.getOrElse(envOrFail(key))
  }
}
