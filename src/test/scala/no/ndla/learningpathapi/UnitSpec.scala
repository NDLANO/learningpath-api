package no.ndla.learningpathapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar


abstract class UnitSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfter {
  LearningpathApiProperties.setProperties(Map(
    "CONTACT_EMAIL" -> Some("ndla@knowit.no"),
    "HOST_ADDR" -> Some("localhost"),
    "DOMAINS" -> Some("localhost"),

    "DB_USER_NAME" -> Some("user"),
    "DB_PASSWORD" -> Some("password"),
    "DB_RESOURCE" -> Some("dbresource"),
    "DB_SERVER" -> Some("dbserver"),
    "DB_PORT" -> Some("1"),
    "DB_SCHEMA" -> Some("dbschema")
  ))
}