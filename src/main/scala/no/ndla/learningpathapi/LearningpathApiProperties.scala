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
