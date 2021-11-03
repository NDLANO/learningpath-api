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

import scala.util.Properties._
import scala.util.{Failure, Success}

object LearningpathApiProperties extends LazyLogging {
  val IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  val Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "learningpath-api"
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val ApplicationPort: Int = propOrElse("APPLICATION_PORT", "80").toInt
  val DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", Language.NORWEGIAN_BOKMAL)
  val ContactName: String = propOrElse("CONTACT_NAME", "NDLA")
  val ContactUrl: String = propOrElse("CONTACT_URL", "ndla.no")
  val ContactEmail: String = propOrElse("CONTACT_EMAIL", "support+api@ndla.no")
  val TermsUrl: String = propOrElse("TERMS_URL", "https://om.ndla.no/tos")

  lazy val Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))

  val MetaMaxConnections = 10

  val SearchIndex: String = propOrElse("SEARCH_INDEX_NAME", "learningpaths")
  val SearchDocument = "learningpath"
  val DefaultPageSize = 10
  val MaxPageSize = 10000
  val IndexBulkSize = 1000

  val ApiGatewayHost: String = propOrElse("API_GATEWAY_HOST", "api-gateway.ndla-local")
  val ImageApiHost: String = propOrElse("IMAGE_API_HOST", "image-api.ndla-local")
  val InternalImageApiUrl = s"$ImageApiHost/image-api/v2/images"
  val SearchApiHost: String = propOrElse("SEARCH_API_HOST", "search-api.ndla-local")

  val NdlaFrontendHost: String = propOrElse("NDLA_FRONTEND_HOST", Environment match {
    case "prod"  => "ndla.no"
    case "local" => "localhost:30017"
    case _       => s"$Environment.ndla.no"
  })

  val NdlaFrontendProtocol: String = propOrElse("NDLA_FRONTEND_PROTOCOL", Environment match {
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

  val NdlaFrontendHostNames: Set[String] = Set(
    "ndla.no",
    "www.ndla.no",
    s"ndla-frontend.api.ndla.no",
    "localhost",
  ) ++
    EnvironmentUrls(Environment) ++
    EnvironmentUrls("test") ++
    EnvironmentUrls("staging")

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

  def MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String = prop(PropertyKeys.MetaSchemaKey)

  val SearchServer: String =
    propOrElse("SEARCH_SERVER", "http://search-learningpath-api.ndla-local")

  val RunWithSignedSearchRequests: Boolean =
    propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    propOrNone(key) match {
      case Some(prop) => prop
      case _          => default
    }
  }

}
