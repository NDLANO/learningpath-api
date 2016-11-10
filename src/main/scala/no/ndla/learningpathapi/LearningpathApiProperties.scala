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
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")

  val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  val MetaResource = prop(PropertyKeys.MetaResourceKey)
  val MetaServer = prop(PropertyKeys.MetaServerKey)
  val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  val MetaSchema = prop(PropertyKeys.MetaSchemaKey)

  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-learningpath-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    secrets.get(key).flatten match {
      case Some(secret) => secret
      case None =>
        envOrNone(key) match {
          case Some(env) => env
          case None => default
        }
    }
  }
}
