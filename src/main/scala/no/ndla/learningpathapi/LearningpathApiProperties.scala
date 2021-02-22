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
import no.ndla.network.{AuthUser, Domains}
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets

import scala.util.Properties._
import scala.util.{Failure, Success}

object LearningpathApiProperties extends LazyLogging {
  val IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "learningpath-api"
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val ApplicationPort = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "support+api@ndla.no"

  val Domain = Domains.get(Environment)

  val MetaMaxConnections = 10

  val SearchIndex = propOrElse("SEARCH_INDEX_NAME", "learningpaths")
  val SearchDocument = "learningpath"
  val DefaultPageSize = 10
  val MaxPageSize = 10000
  val IndexBulkSize = 1000

  val ArticleImportHost =
    propOrElse("ARTICLE_IMPORT_HOST", "article-import.ndla-local")
  val ApiGatewayHost = propOrElse("API_GATEWAY_HOST", "api-gateway.ndla-local")
  val ImageApiHost = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  val InternalImageApiUrl = s"$ImageApiHost/image-api/v2/images"
  val SearchApiHost = propOrElse("SEARCH_API_HOST", "search-api.ndla-local")

  val NdlaFrontendHost = propOrElse("NDLA_FRONTEND_HOST", Environment match {
    case "prod"  => "ndla.no"
    case "local" => "localhost:30017"
    case _       => s"$Environment.ndla.no"
  })

  val NdlaFrontendProtocol = propOrElse("NDLA_FRONTEND_PROTOCOL", Environment match {
    case "local" => "http"
    case _       => "https"
  })

  def EnvironmentUrls(env: String): Set[String] = {
    Set(
      s"$env.ndla.no",
      s"www.$env.ndla.no",
      s"ndla-frontend.$env.api.ndla.no"
    )
  }

  val NdlaFrontendHostNames = Set(
    "ndla.no",
    "www.ndla.no",
    s"ndla-frontend.api.ndla.no",
    "localhost",
  ) ++
    EnvironmentUrls(Environment) ++
    EnvironmentUrls("test") ++
    EnvironmentUrls("staging")

  val DefaultLanguage = Language.NORWEGIAN_BOKMAL
  val UsernameHeader = "X-Consumer-Username"

  val ElasticSearchIndexMaxResultWindow = 10000
  val ElasticSearchScrollKeepAlive = "1m"
  val InitialScrollContextKeywords = List("0", "initial", "start", "first")

  val BasicHtmlTags = List("b",
                           "blockquote",
                           "br",
                           "cite",
                           "code",
                           "dd",
                           "dl",
                           "dt",
                           "em",
                           "i",
                           "li",
                           "ol",
                           "p",
                           "pre",
                           "q",
                           "small",
                           "strike",
                           "strong",
                           "sub",
                           "sup",
                           "u",
                           "ul")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  def MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource = prop(PropertyKeys.MetaResourceKey)
  def MetaServer = prop(PropertyKeys.MetaServerKey)
  def MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema = prop(PropertyKeys.MetaSchemaKey)

  val SearchServer =
    propOrElse("SEARCH_SERVER", "http://search-learningpath-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")

  val RunWithSignedSearchRequests =
    propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  lazy val secrets = {
    val SecretsFile = "learningpath-api.secrets"
    readSecrets(SecretsFile, Set("LEARNINGPATH_CLIENT_ID", "LEARNINGPATH_CLIENT_SECRET"), readDBCredentials = true) match {
      case Success(values) => values
      case Failure(exception) =>
        throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
    }
  }

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    propOrNone(key) match {
      case Some(prop)            => prop
      case None if !IsKubernetes => secrets.get(key).flatten.getOrElse(default)
      case _                     => default
    }
  }

}
