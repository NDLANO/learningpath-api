package no.ndla.learningpathapi

import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

object LearningpathApiProperties extends LazyLogging {


  val EnvironmentFile = "/learningpath-api.env"
  val LearningpathApiProps = readPropertyFile()

  val ApplicationPort = 80
  val ContactEmail = get("CONTACT_EMAIL")
  val HostAddr = get("HOST_ADDR")
  val Domains = get("DOMAINS").split(",") ++ Array(HostAddr)

  val MetaUserName = get("DB_USER_NAME")
  val MetaPassword = get("DB_PASSWORD")
  val MetaResource = get("DB_RESOURCE")
  val MetaServer = get("DB_SERVER")
  val MetaPort = getInt("DB_PORT")
  val MetaInitialConnections = getInt("DB_INITIAL_CONNECTIONS")
  val MetaMaxConnections = getInt("DB_MAX_CONNECTIONS")
  val MetaSchema = get("DB_SCHEMA")

  val SearchHost = "search-engine"
  val SearchPort = get("SEARCH_ENGINE_ENV_TCP_PORT")
  var SearchClusterName = get("SEARCH_ENGINE_ENV_CLUSTER_NAME")
  val SearchIndex = get("SEARCH_INDEX")
  val SearchDocument = get("SEARCH_DOCUMENT")
  val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  val Published = "PUBLISHED"
  val Private = "PRIVATE"
  val External = "EXTERNAL"
  val CreatedByNDLA = "CREATED_BY_NDLA"
  val VerifiedByNDLA = "VERIFIED_BY_NDLA"

  val UsernameHeader = "X-Consumer-Username"

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

  private def readPropertyFile(): Map[String, Option[String]] = {
    val keys = Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().withFilter(line => line.matches("^\\w+$"))
    keys.map(key => key -> scala.util.Properties.envOrNone(key)).toMap
  }
}
