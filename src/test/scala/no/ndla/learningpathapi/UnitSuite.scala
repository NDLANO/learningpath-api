package no.ndla.learningpathapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar


abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfter {
  val DEFAULT_PAGE_SIZE = 12
  val MAX_PAGE_SIZE = 548

  LearningpathApiProperties.setProperties(Map(
    "CONTACT_EMAIL" -> Some("ndla@knowit.no"),
    "HOST_ADDR" -> Some("localhost"),
    "DOMAINS" -> Some("localhost"),

    "DB_USER_NAME" -> Some("user"),
    "DB_PASSWORD" -> Some("password"),
    "DB_RESOURCE" -> Some("dbresource"),
    "DB_SERVER" -> Some("dbserver"),
    "DB_PORT" -> Some("1"),
    "DB_SCHEMA" -> Some("dbschema"),

    "SEARCH_ENGINE_ENV_TCP_PORT" -> Some("9300"),
    "SEARCH_ENGINE_ENV_CLUSTER_NAME" -> Some("search-engine"),
    "SEARCH_INDEX" -> Some("learningpaths"),
    "SEARCH_DOCUMENT" -> Some("learningpath"),
    "SEARCH_DEFAULT_PAGE_SIZE" -> Some(s"$DEFAULT_PAGE_SIZE"),
    "SEARCH_MAX_PAGE_SIZE" -> Some(s"$MAX_PAGE_SIZE"),
    "INDEX_BULK_SIZE" -> Some("500")
  ))
}