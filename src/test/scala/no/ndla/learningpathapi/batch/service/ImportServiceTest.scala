package no.ndla.learningpathapi.batch.service

import java.util.Date

import no.ndla.learningpathapi.UnitSuite
import no.ndla.learningpathapi.batch.{BatchTestEnvironment, Node, Package, Step}
import no.ndla.learningpathapi.model.domain._
import org.mockito.Matchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

class ImportServiceTest extends UnitSuite with BatchTestEnvironment {

  var service:ImportService = _

  override def beforeEach() = {
    service = new ImportService
  }

  test("That getTranslations returns empty list when no translations") {
    service.getTranslations(
      nodeWithNidAndTnid(1,0),
      List(nodeWithNidAndTnid(2,0), nodeWithNidAndTnid(3,0))
    ) should equal(List())
  }

  test("That getTranslations returns node with tnid matching nid") {
    val translation1 = nodeWithNidAndTnid(2,1)
    val translation2 = nodeWithNidAndTnid(3,1)
    val packageFor1 = packageWithNodeId(translation1.nid)
    val packageFor2 = packageWithNodeId(translation2.nid)

    when(packageData.packageFor(translation1)).thenReturn(Some(packageFor1))
    when(packageData.packageFor(translation2)).thenReturn(Some(packageFor2))

    val translations = service.getTranslations(
      nodeWithNidAndTnid(1,0),
      List(nodeWithNidAndTnid(2,1), nodeWithNidAndTnid(3,1))
    )
    translations.size should be(2)
    translations.head.get.nodeId should be(2)
    translations.last.get.nodeId should be(3)
  }

  test("That tidyUpDescription returns emtpy string for null") {
    service.tidyUpDescription(null) should equal("")
  }

  test("That tidyUpDescription removes \\r \\t and \\n, but nothing else") {
    service.tidyUpDescription("1\r+\t1\n=\t2") should equal("1 + 1 = 2")
  }

  test("That descriptionsAsList returns descriptions of translations when origin-step is None") {
    val descriptions = service.descriptionAsList(None, List(
      stepWithDescriptionAndLanguage(Some("Beskrivelse1"), "nb"),
      stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn")))

    descriptions.size should be(2)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse1,Beskrivelse2")
    descriptions.map(_.language.get).mkString(",") should equal("nb,nn")
  }

  test("That descriptionsAsList returns origin-step description and all translations") {
    val descriptions = service.descriptionAsList(Some(stepWithDescriptionAndLanguage(Some("Beskrivelse1"), "nb")), List(
      stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn"),
      stepWithDescriptionAndLanguage(Some("Beskrivelse3"), "en")
    ))

    descriptions.size should be(3)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse1,Beskrivelse2,Beskrivelse3")
    descriptions.map(_.language.get).mkString(",") should equal("nb,nn,en")
  }

  test("That descriptionsAsList returns translations when origin-step description is None") {
    val descriptions = service.descriptionAsList(Some(stepWithDescriptionAndLanguage(None, "nb")), List(
      stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn"),
      stepWithDescriptionAndLanguage(Some("Beskrivelse3"), "en")
    ))

    descriptions.size should be(2)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse2,Beskrivelse3")
    descriptions.map(_.language.get).mkString(",") should equal("nn,en")
  }

  test("That embedUrlsAsList returns origin-step embedUrl and all translations") {
    val embedUrls = service.embedUrlsAsList(stepWithEmbedUrlAndLanguage(Some("http://ndla.no/1"), "nb"), List(
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/2"), "nn"),
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/3"), "en")
    ))

    embedUrls.size should be (3)
    embedUrls.map(_.url).mkString(",") should equal ("http://ndla.no/1,http://ndla.no/2,http://ndla.no/3")
    embedUrls.map(_.language.get).mkString(",") should equal ("nb,nn,en")
  }

  test("That embedUrlsAsList returns translations when origin-step description is None") {
    val embedUrls = service.embedUrlsAsList(stepWithEmbedUrlAndLanguage(None, "nb"), List(
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/2"), "nn"),
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/3"), "en")
    ))

    embedUrls.size should be (2)
    embedUrls.map(_.url).mkString(",") should equal ("http://ndla.no/2,http://ndla.no/3")
    embedUrls.map(_.language.get).mkString(",") should equal ("nn,en")
  }


  test("That importNode inserts for a new node") {
    val pakke = packageWithNodeId(1)
    val steps = List(stepWithDescriptionAndLanguage(Some("Beskrivelse"), "nb"))

    when(packageData.stepsForPackage(pakke)).thenReturn(steps)
    when(packageData.getTranslationSteps(any[List[Option[Package]]], any[Int])).thenReturn(List())
    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[Option[String]])).thenReturn(None)

    service.importNode(Some(pakke), List(), None, "test")

    verify(learningPathRepository, times(1)).insert(any[LearningPath])
  }

  test("That importNode updates for an existing node") {
    val pakke = packageWithNodeId(1)
    val steps = List(stepWithDescriptionAndLanguage(Some("Beskrivelse"), "nb"))
    val sanders = Author("author", "Crazy Bernie")
    val license = License("publicdomain", "Public Domain", "https://creativecommons.org/about/pdm")
    val copyright = Copyright(license, "", List(sanders))
    val existingLearningPath = LearningPath(Some(1), Some(1), Some("1"), None, List(), List(), None, Some(1), LearningPathStatus.PRIVATE, LearningPathVerificationStatus.CREATED_BY_NDLA, new Date(), List(), "", copyright)

    when(packageData.stepsForPackage(pakke)).thenReturn(steps)
    when(packageData.getTranslationSteps(any[List[Option[Package]]], any[Int])).thenReturn(List())
    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[Option[String]])).thenReturn(Some(existingLearningPath))
    when(learningPathRepository.learningStepWithExternalIdAndForLearningPath(any[Option[String]], any[Option[Long]])(any[DBSession])).thenReturn(None)

    service.importNode(Some(pakke), List(), None, "test")

    verify(learningPathRepository, times(1)).update(any[LearningPath])
  }

  test("That asLearningPath returns expected values for test-environment") {
    val pakke = packageWithNodeId(1).copy(durationHours = 1, durationMinutes = 1)
    val titles = List(Title("Tittel", Some("nb")))
    val descriptions = List(Description("Beskrivelse", Some("nb")))
    val tags = List(LearningPathTags(Seq("Tag"), Some("nb")))
    val steps = List(LearningStep(None, None, None, None, 1, List(Title("StegTittel", Some("nb"))), List(Description("StegBeskrivelse", Some("nb"))), List(), StepType.INTRODUCTION, None))
    val imageUrl = None


    Map("test" -> ChristerTest, "staging" -> ChristerStaging, "prod" -> ChristerProd, "etannetmiljø" -> ChristerTest).foreach{ case (environment, expectedOwner) =>
      val learningPath = service.asLearningPath(pakke, titles, descriptions, tags, steps, imageUrl, environment)

      learningPath.owner should be (expectedOwner)
      learningPath.duration should be (Some(61))
    }

  }

  private def nodeWithNidAndTnid(nid: Long, tnid: Long): Node = Node(nid, tnid, "en", "Tittel", 1, None, "Beskrivelse")
  private def packageWithNodeId(nid: Long):Package = Package(1, new Date(), 1, "Tittel", 1, 1, "en", nid, "Beskrivelse")
  private def stepWithDescriptionAndLanguage(description: Option[String], language: String): Step = Step(1, 1, 1, "Tittel", 1, 1, None, description, language)
  private def stepWithEmbedUrlAndLanguage(embedUrl: Option[String], language: String): Step = Step(1, 1, 1, "Tittel", 1, 1, embedUrl, None, language)
}
