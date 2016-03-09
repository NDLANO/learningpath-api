package no.ndla.learningpathapi

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.io.Source

object LearningpathApiProperties extends LazyLogging {

  var LearningpathApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  val ApplicationPort = 80
  def ContactEmail = get("CONTACT_EMAIL")
  def HostAddr = get("HOST_ADDR")
  def Domains = get("DOMAINS").split(",") ++ Array(HostAddr)

  def MetaUserName = get("DB_USER_NAME")
  def MetaPassword = get("DB_PASSWORD")
  def MetaResource = get("DB_RESOURCE")
  def MetaServer = get("DB_SERVER")
  def MetaPort = getInt("DB_PORT")
  def MetaSchema = get("DB_SCHEMA")
  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  val SearchHost = "search-engine"
  def SearchPort = get("SEARCH_ENGINE_ENV_TCP_PORT")
  def SearchClusterName = get("SEARCH_ENGINE_ENV_CLUSTER_NAME")
  def SearchIndex = get("SEARCH_INDEX")
  def SearchDocument = get("SEARCH_DOCUMENT")
  def DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  def MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  def IndexBulkSize = getInt("INDEX_BULK_SIZE")

  val AuthHost = "auth"
  val OEmbedHost = "oembed-proxy"
  val DefaultLanguage = "nb"
  val UsernameHeader = "X-Consumer-Username"

  val BasicHtmlTags = List("b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em",
    "i", "li", "ol", "p", "pre", "q", "small", "strike", "strong",
    "sub", "sup", "u", "ul")

  def setProperties(properties: Map[String, Option[String]]) = {
    properties.foreach(prop => LearningpathApiProps.put(prop._1, prop._2))
  }

  def verify() = {
    val missingProperties = LearningpathApiProps.filter(entry => entry._2.isEmpty).toList
    if (missingProperties.nonEmpty) {
      missingProperties.foreach(entry => logger.error("Missing required environment variable {}", entry._1))

      logger.error("Shutting down.")
      System.exit(1)
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
}

object PropertiesLoader {
  val EnvironmentFile = "/learningpath-api.env"

  private def readPropertyFile(): Map[String, Option[String]] = {
    val keys = Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().withFilter(line => line.matches("^\\w+$"))
    keys.map(key => key -> scala.util.Properties.envOrNone(key)).toMap
  }

  def load() = {
    LearningpathApiProperties.setProperties(readPropertyFile())
    LearningpathApiProperties.verify()
  }
}
