/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date
import javax.servlet.http.HttpServletRequest

import com.netaporter.uri.dsl._
import no.ndla.learningpathapi.integration.ImageMetaInformation
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.api.CoverPhoto
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import no.ndla.network.ApplicationUrl
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.Success

class ConverterServiceTest extends UnitSuite with UnitTestEnvironment {
  val clinton = api.Author("author", "Crooked Hillary")
  val license = api.License("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
  val copyright = api.Copyright(license, List(clinton))

  val apiLearningPath = api.LearningPathV2(
    1,
    1,
    None,
    api.Title("Tittel", "nb"),
    api.Description("Beskrivelse", "nb"),
    "",
    List(),
    "",
    None,
    None,
    "PRIVATE",
    "CREATED_BY_NDLA",
    new Date(),
    api.LearningPathTags(List(), "nb"),
    copyright,
    true,
    List("nb"),
    None,
    None
  )
  val domainLearningStep = LearningStep(None, None, None, None, 1, List(), List(), List(), StepType.INTRODUCTION, None)

  val domainLearningStep2 = LearningStep(Some(1),
                                         Some(1),
                                         None,
                                         None,
                                         1,
                                         List(Title("tittel", "nb")),
                                         List(Description("deskripsjon", "nb")),
                                         List(),
                                         StepType.INTRODUCTION,
                                         None)
  val apiTags = List(api.LearningPathTags(Seq("tag"), Language.DefaultLanguage))

  val randomDate = DateTime.now().toDate
  var service: ConverterService = _

  val domainLearningPath = LearningPath(
    Some(1),
    Some(1),
    None,
    None,
    List(Title("tittel", Language.DefaultLanguage)),
    List(Description("deskripsjon", Language.DefaultLanguage)),
    None,
    Some(60),
    LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.CREATED_BY_NDLA,
    randomDate,
    List(LearningPathTags(List("tag"), Language.DefaultLanguage)),
    "me",
    Copyright("by", List.empty),
    List.empty
  )

  override def beforeEach() = {
    service = new ConverterService
  }

  test("asApiLearningpathV2 converts domain to api LearningPathV2") {
    val expected = Some(
      api.LearningPathV2(
        1,
        1,
        None,
        api.Title("tittel", Language.DefaultLanguage),
        api.Description("deskripsjon", Language.DefaultLanguage),
        "null1",
        List.empty,
        "null1/learningsteps",
        None,
        Some(60),
        LearningPathStatus.PRIVATE.toString,
        LearningPathVerificationStatus.CREATED_BY_NDLA.toString,
        randomDate,
        api.LearningPathTags(Seq("tag"), Language.DefaultLanguage),
        api.Copyright(api.License("by",
                                  Some("Creative Commons Attribution 2.0 Generic"),
                                  Some("https://creativecommons.org/licenses/by/2.0/")),
                      List.empty),
        canEdit = true,
        List("nb", "en"),
        None,
        None
      ))
    service.asApiLearningpathV2(domainLearningPath.copy(title = domainLearningPath.title :+ Title("test", "en")),
                                Language.DefaultLanguage,
                                UserInfo("me", Set.empty)) should equal(expected)
  }

  test("asApiLearningpathSummaryV2 converts domain to api LearningpathSummaryV2") {
    val expected = Success(
      api.LearningPathSummaryV2(
        1,
        api.Title("tittel", Language.DefaultLanguage),
        api.Description("deskripsjon", Language.DefaultLanguage),
        api.Introduction("", Language.DefaultLanguage),
        "null1",
        None,
        Some(60),
        LearningPathStatus.PRIVATE.toString,
        randomDate,
        api.LearningPathTags(Seq("tag"), Language.DefaultLanguage),
        api.Copyright(api.License("by",
                                  Some("Creative Commons Attribution 2.0 Generic"),
                                  Some("https://creativecommons.org/licenses/by/2.0/")),
                      List.empty),
        List("nb", "en"),
        None,
        None
      ))
    service.asApiLearningpathSummaryV2(domainLearningPath.copy(title = domainLearningPath.title :+ Title("test", "en"))) should equal(
      expected)
  }

  test("asApiLearningStepV2 converts domain learningstep to api LearningStepV2") {
    val learningstep = Some(
      api.LearningStepV2(
        1,
        1,
        1,
        api.Title("tittel", Language.DefaultLanguage),
        Some(api.Description("deskripsjon", Language.DefaultLanguage)),
        None,
        showTitle = false,
        "INTRODUCTION",
        None,
        "null1/learningsteps/1",
        canEdit = true,
        "ACTIVE",
        Seq(Language.DefaultLanguage)
      ))
    service.asApiLearningStepV2(domainLearningStep2,
                                domainLearningPath,
                                Language.DefaultLanguage,
                                UserInfo("me", Set.empty)) should equal(learningstep)
  }

  test("asApiLearningStepSummaryV2 converts domain learningstep to LearningStepSummaryV2") {
    val expected = Some(
      api.LearningStepSummaryV2(
        1,
        1,
        api.Title("tittel", Language.DefaultLanguage),
        "INTRODUCTION",
        "null1/learningsteps/1"
      ))

    service.asApiLearningStepSummaryV2(domainLearningStep2, domainLearningPath, Language.DefaultLanguage) should equal(
      expected)
  }

  test("asApiLearningStepSummaryV2 returns what we have when not supported language is given") {
    val expected = Some(
      api.LearningStepSummaryV2(
        1,
        1,
        api.Title("tittel", Language.DefaultLanguage),
        "INTRODUCTION",
        "null1/learningsteps/1"
      ))

    service.asApiLearningStepSummaryV2(domainLearningStep2, domainLearningPath, "somerandomlanguage") should equal(
      expected)
  }

  test("asApiLearningPathTagsSummary converts api LearningPathTags to api LearningPathTagsSummary") {
    val expected =
      Some(api.LearningPathTagsSummary(Language.DefaultLanguage, Seq(Language.DefaultLanguage), Seq("tag")))
    service.asApiLearningPathTagsSummary(apiTags, Language.DefaultLanguage) should equal(expected)
  }

  test("That createUrlToLearningPath does not include private in path for private learningpath") {
    val httpServletRequest = mock[HttpServletRequest]
    when(httpServletRequest.getServerPort).thenReturn(80)
    when(httpServletRequest.getScheme).thenReturn("http")
    when(httpServletRequest.getServerName).thenReturn("localhost")
    when(httpServletRequest.getServletPath).thenReturn("/servlet")

    ApplicationUrl.set(httpServletRequest)
    service.createUrlToLearningPath(apiLearningPath.copy(status = "PRIVATE")) should equal("http://localhost/servlet/1")
  }

  test("That asApiIntroduction returns an introduction for a given step") {
    val introductions = service.getApiIntroduction(
      Seq(
        domainLearningStep.copy(
          description = Seq(
            Description("Introduksjon på bokmål", "nb"),
            Description("Introduksjon på nynorsk", "nn"),
            Description("Introduction in english", "en")
          ))))

    introductions.size should be(3)
    introductions.find(_.language.contains("nb")).map(_.introduction) should be(Some("Introduksjon på bokmål"))
    introductions.find(_.language.contains("nn")).map(_.introduction) should be(Some("Introduksjon på nynorsk"))
    introductions.find(_.language.contains("en")).map(_.introduction) should be(Some("Introduction in english"))
  }

  test("That asApiIntroduction returns empty list if no descriptions are available") {
    val introductions = service.getApiIntroduction(Seq(domainLearningStep))
    introductions.size should be(0)
  }

  test("That asApiIntroduction returns an empty list if given a None") {
    service.getApiIntroduction(Seq()) should equal(Seq())
  }

  test("asApiLicense returns a License object for a given valid license") {
    service.asApiLicense("by") should equal(
      api.License("by",
                  Option("Creative Commons Attribution 2.0 Generic"),
                  Some("https://creativecommons.org/licenses/by/2.0/")))
  }

  test("asApiLicense returns a default license object for an invalid license") {
    service.asApiLicense("invalid") should equal(api.License("invalid", Option("Invalid license"), None))
  }

  test("asEmbedUrl returns embedUrl if embedType is oembed") {
    service.asEmbedUrlV2(api.EmbedUrlV2("http://test.no/2/oembed/", "oembed"), "nb") should equal(
      EmbedUrl("http://test.no/2/oembed/", "nb", EmbedType.OEmbed))
  }

  test("asEmbedUrl throws error if an not allowed value for embedType is used") {
    assertResult("Validation Error") {
      intercept[ValidationException] {
        service.asEmbedUrlV2(api.EmbedUrlV2("http://test.no/2/oembed/", "test"), "nb")
      }.getMessage()
    }
  }

  test("asCoverPhoto converts an image id to CoverPhoto") {
    val imageMeta =
      ImageMetaInformation("1",
                           "http://image-api.ndla-local/image-api/v2/images/1",
                           "http://image-api.ndla-local/image-api/raw/1.jpg",
                           1024,
                           "something")
    val expectedResult =
      CoverPhoto("http://api-gateway.ndla-local/image-api/raw/1.jpg",
                 "http://api-gateway.ndla-local/image-api/v2/images/1")
    when(imageApiClient.imageMetaOnUrl(any[String])).thenReturn(Some(imageMeta))
    val Some(result) = service.asCoverPhoto("1")

    result should equal(expectedResult)
  }

  test("asDomainEmbed should only use context path if hostname is ndla-frontend but full url when not") {
    val url = "https://beta.ndla.no/subjects/resource:1234?a=test"
    service.asDomainEmbedUrl(api.EmbedUrlV2(url, "oembed"), "nb") should equal(
      EmbedUrl(s"/subjects/resource:1234/?a=test", "nb", EmbedType.OEmbed))

    val externalUrl = "https://youtube.com/watch?v=8992BFHks"
    service.asDomainEmbedUrl(api.EmbedUrlV2(externalUrl, "oembed"), "nb") should equal(
      EmbedUrl(externalUrl, "nb", EmbedType.OEmbed))
  }

  test("That mergeLanguageFields returns original list when updated is empty") {
    val existing = Seq(Title("Tittel 1", "nb"), Title("Tittel 2", "nn"), Title("Tittel 3", "unknown"))
    service.mergeLanguageFields(existing, Seq()) should equal(existing)
  }

  test("That mergeLanguageFields updated the english title only when specified") {
    val tittel1 = Title("Tittel 1", "nb")
    val tittel2 = Title("Tittel 2", "nn")
    val tittel3 = Title("Tittel 3", "en")
    val oppdatertTittel3 = Title("Title 3 in english", "en")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel3)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel2, oppdatertTittel3))
  }

  test("That mergeLanguageFields removes a title that is empty") {
    val tittel1 = Title("Tittel 1", "nb")
    val tittel2 = Title("Tittel 2", "nn")
    val tittel3 = Title("Tittel 3", "en")
    val tittelToRemove = Title("", "nn")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(tittelToRemove)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3))
  }

  test("That mergeLanguageFields updates the title with no language specified") {
    val tittel1 = Title("Tittel 1", "nb")
    val tittel2 = Title("Tittel 2", "unknown")
    val tittel3 = Title("Tittel 3", "en")
    val oppdatertTittel2 = Title("Tittel 2 er oppdatert", "unknown")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel2)

    service.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3, oppdatertTittel2))
  }

  test("That mergeLanguageFields also updates the correct description") {
    val desc1 = Description("Beskrivelse 1", "nb")
    val desc2 = Description("Beskrivelse 2", "unknown")
    val desc3 = Description("Beskrivelse 3", "en")
    val oppdatertDesc2 = Description("Beskrivelse 2 er oppdatert", "unknown")

    val existing = Seq(desc1, desc2, desc3)
    val updated = Seq(oppdatertDesc2)
    service.mergeLanguageFields(existing, updated) should equal(Seq(desc1, desc3, oppdatertDesc2))
  }

  test("That a apiLearningPath should only contain ownerId if admin") {
    val noAdmin = service.asApiLearningpathV2(domainLearningPath, "nb", UserInfo(domainLearningPath.owner, Set.empty))
    val admin = service.asApiLearningpathV2(domainLearningPath, "nb", UserInfo("kwakk", Set(LearningPathRole.ADMIN)))

    noAdmin.get.ownerId should be(None)
    admin.get.ownerId.get should be(domainLearningPath.owner)
  }

}
