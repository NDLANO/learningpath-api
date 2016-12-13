/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.model.api.License
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.network.model.HttpRequestException
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.{Failure, Success}
import scalaj.http.HttpRequest

class MappingApiClientTest extends UnitSuite with TestEnvironment {

  var client = new MappingApiClient
  val sampleLicenses = Seq(LicenseDefinition("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")),
    LicenseDefinition("by-sa", Some("Creative Commons Attribution-ShareAlike 2.0 Generic"), Some("https://creativecommons.org/licenses/by-sa/2.0/")),
    LicenseDefinition("by-nc", Some("Creative Commons Attribution-NonCommercial 2.0 Generic"), Some("https://creativecommons.org/licenses/by-nc/2.0/")),
    LicenseDefinition("nlod", Some("Norsk lisens for offentlige data"), Some("http://data.norge.no/nlod/no/1.0")))

  val sampleMappings = Map(
    "nob" -> "nb",
    "eng" -> "en",
    "fra" -> "fr"
  )

  override def beforeEach() {
    client = new MappingApiClient
  }

  test("That getLicenseDefinition returns a license if found") {
    val expectedResult = Some(License("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")))
    when(ndlaClient.fetch[Seq[LicenseDefinition]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Seq[LicenseDefinition]]])).thenReturn(Success(sampleLicenses))
    client.getLicense("by") should equal(expectedResult)
  }

  test("That getLicenseDefinition returns None if license is not found") {
    client.getLicense("garbage") should equal(None)
  }

  test("That getLicenseDefinition throws an exception if the http call failed") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(ndlaClient.fetch[Seq[LicenseDefinition]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Seq[LicenseDefinition]]])).thenReturn(Failure(exception))

    intercept[HttpRequestException] {
      client.getLicense("garbage")
    }
  }

  test("That getCreativeCommonLicenses only returns licenses starting with by") {
    val expectedResult = Seq(License("by", Some("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")),
      License("by-sa", Some("Creative Commons Attribution-ShareAlike 2.0 Generic"), Some("https://creativecommons.org/licenses/by-sa/2.0/")),
      License("by-nc", Some("Creative Commons Attribution-NonCommercial 2.0 Generic"), Some("https://creativecommons.org/licenses/by-nc/2.0/")))
    when(ndlaClient.fetch[Seq[LicenseDefinition]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Seq[LicenseDefinition]]])).thenReturn(Success(sampleLicenses))
    client.getCreativeCommonLicenses should equal(expectedResult)
  }

  test("That get6391CodeFor6392Code returns a language code if it exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.get6391CodeFor6392Code("nob") should equal (Some("nb"))
  }

  test("That get6391CodeFor6392Code returns None if the language code does not exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.get6391CodeFor6392Code("garbage") should equal (None)
  }

  test("That get6391CodeFor6392Code throws an exception if the http call failed") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Failure(exception))

    intercept[HttpRequestException] {
      client.getLicense("garbage")
    }
  }

  test("That languageCodeSupported6392 returns true if it exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.languageCodeSupported6392("nob") should equal (true)
  }

  test("That languageCodeSupported6392 returns false if the language code does not exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.languageCodeSupported6392("garbage") should equal (false)
  }

  test("That languageCodeSupported6391 returns true if it exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.languageCodeSupported6391("nb") should equal (true)
  }

  test("That languageCodeSupported6391 returns false if the language code does not exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.languageCodeSupported6391("garbage") should equal (false)
  }

  test("That languageCodeSupported throws an exception if the http call failed") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Failure(exception))

    intercept[HttpRequestException] {
      client.languageCodeSupported6392("garbage")
    }
  }

}