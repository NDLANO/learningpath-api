/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.integration.{MainPackageImport, Package, Step}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import scalikejdbc.DBSession

class ImportServiceTest extends UnitSuite with UnitTestEnvironment {

  override val importService = new ImportService

  test("That tidyUpDescription returns emtpy string for null") {
    importService.tidyUpDescription(null) should equal("")
  }

  test("That tidyUpDescription removes \\r \\t and \\n, but nothing else") {
    importService.tidyUpDescription("1\r+\t1\n=\t2") should equal("1 + 1 = 2")
  }

  test("That descriptionsAsList returns descriptions of translations when origin-step is None") {
    val descriptions = importService.descriptionAsList(None, List(
      stepWithDescriptionAndLanguage(Some("Beskrivelse1"), "nb"),
      stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn")))

    descriptions.size should be(2)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse1,Beskrivelse2")
    descriptions.map(_.language).mkString(",") should equal("nb,nn")
  }

  test("That descriptionsAsList returns origin-step description and all translations") {
    val descriptions = importService.descriptionAsList(Some(stepWithDescriptionAndLanguage(Some("Beskrivelse1"), "nb")), List(
      stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn"),
      stepWithDescriptionAndLanguage(Some("Beskrivelse3"), "en")
    ))

    descriptions.size should be(3)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse1,Beskrivelse2,Beskrivelse3")
    descriptions.map(_.language).mkString(",") should equal("nb,nn,en")
  }

  test("That descriptionsAsList returns translations when origin-step description is None") {
    val descriptions = importService.descriptionAsList(Some(stepWithDescriptionAndLanguage(None, "nb")), List(
      stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn"),
      stepWithDescriptionAndLanguage(Some("Beskrivelse3"), "en")
    ))

    descriptions.size should be(2)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse2,Beskrivelse3")
    descriptions.map(_.language).mkString(",") should equal("nn,en")
  }

  test("That embedUrlsAsList returns origin-step embedUrl and all translations") {
    val embedUrls = importService.embedUrlsAsList(stepWithEmbedUrlAndLanguage(Some("http://ndla.no/1"), "nb"), List(
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/2"), "nn"),
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/3"), "en")
    ))

    embedUrls.size should be(3)
    embedUrls.map(_.url).mkString(",") should equal("https://ndla.no/1,https://ndla.no/2,https://ndla.no/3")
    embedUrls.map(_.language).mkString(",") should equal("nb,nn,en")
  }

  test("That embedUrlsAsList returns translations when origin-step description is None") {
    val embedUrls = importService.embedUrlsAsList(stepWithEmbedUrlAndLanguage(None, "nb"), List(
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/2"), "nn"),
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/3"), "en")
    ))

    embedUrls.size should be(2)
    embedUrls.map(_.url).mkString(",") should equal("https://ndla.no/2,https://ndla.no/3")
    embedUrls.map(_.language).mkString(",") should equal("nn,en")
  }

  test("That importNode inserts for a new node") {
    val mainImport = MainPackageImport(packageWithNodeId(1), Seq())

    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[Option[String]])).thenReturn(None)

    importService.upload(mainImport)

    verify(learningPathRepository, times(1)).insert(any[LearningPath])
  }

  test("That importNode updates for an existing node") {
    val mainImport = MainPackageImport(packageWithNodeId(1), Seq())
    val sanders = Author("author", "Crazy Bernie")
    val license = "publicdomain"
    val copyright = Copyright(license, List(sanders))
    val existingLearningPath = LearningPath(Some(1), Some(1), Some("1"), None, List(), List(), None, Some(1), LearningPathStatus.PRIVATE, LearningPathVerificationStatus.CREATED_BY_NDLA, new Date(), List(), "", copyright)

    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[Option[String]])).thenReturn(Some(existingLearningPath))
    when(learningPathRepository.learningStepWithExternalIdAndForLearningPath(any[Option[String]], any[Option[Long]])(any[DBSession])).thenReturn(None)

    importService.upload(mainImport)

    verify(learningPathRepository, times(1)).update(any[LearningPath])
  }

  test("That getOwnerForEnvironment returns expected owner for each environment") {
    val envToOwner = Map("test" -> ChristerTest,
      "staging" -> ChristerStaging,
      "prod" -> ChristerProd,
      "etannetmiljø" -> ChristerTest)

    envToOwner.foreach {
      case (environment, expectedOwner) => importService.getOwnerForEnvironment(environment) should equal(expectedOwner)
    }
  }

  test("That duration is calculated correctly") {
    val pakke = packageWithNodeId(1).copy(durationHours = 1, durationMinutes = 1)
    val learningPath = importService.asLearningPath(pakke, Seq(), Seq(), Seq(), Seq(), None)

    learningPath.duration should be(Some(61))
  }

  private def packageWithNodeId(nid: Long): Package = Package(nid, nid, "nb", "NodeTitle", None, "NodeDescription", 1, new Date(), 1, "PackageTittel", 1, 1, Seq())
  private def stepWithDescriptionAndLanguage(description: Option[String], language: String): Step = Step(1, 1, 1, "Tittel", 1, 1, None, description, None, language)
  private def stepWithEmbedUrlAndLanguage(embedUrl: Option[String], language: String): Step = Step(1, 1, 1, "Tittel", 1, 1, embedUrl, None, None, language)
}
