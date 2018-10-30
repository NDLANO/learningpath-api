/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.caching.Memoize
import no.ndla.learningpathapi.integration.{
  ArticleImportStatus,
  ArticleMigrationContent,
  MainPackageImport,
  MigrationAuthor,
  Package,
  Step,
  TaxonomyResource
}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import no.ndla.network.model.HttpRequestException
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession
import no.ndla.mapping.License.{CC0, PublicDomain, CC_BY_SA}

import scala.util.{Failure, Success}

class ImportServiceTest extends UnitSuite with UnitTestEnvironment {

  val nodeId = "1234"
  def memoizeFunc(n: String) = Set(ArticleMigrationContent(nodeId, nodeId))

  override val importService = new ImportService
  val CLIENT_ID = "Klient1"

  override def beforeEach: Unit = {
    reset(articleImportClient, taxononyApiClient, learningPathRepository)
    when(learningPathRepository.update(any[LearningPath])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => {
        invocation.getArgument[LearningPath](0)
      })
  }

  test("That tidyUpDescription returns emtpy string for null") {
    importService.tidyUpDescription(null) should equal("")
  }

  test("That tidyUpDescription removes \\r \\t and \\n, but nothing else") {
    importService.tidyUpDescription("1\r+\t1\n=\t2") should equal("1 + 1 = 2")
  }

  test("That descriptionsAsList returns descriptions of translations when origin-step is None") {
    val descriptions = importService.descriptionAsList(None,
                                                       List(stepWithDescriptionAndLanguage(Some("Beskrivelse1"), "nb"),
                                                            stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn")))

    descriptions.size should be(2)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse1,Beskrivelse2")
    descriptions.map(_.language).mkString(",") should equal("nb,nn")
  }

  test("That descriptionsAsList returns origin-step description and all translations") {
    val descriptions = importService.descriptionAsList(
      Some(stepWithDescriptionAndLanguage(Some("Beskrivelse1"), "nb")),
      List(
        stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn"),
        stepWithDescriptionAndLanguage(Some("Beskrivelse3"), "en")
      )
    )

    descriptions.size should be(3)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse1,Beskrivelse2,Beskrivelse3")
    descriptions.map(_.language).mkString(",") should equal("nb,nn,en")
  }

  test("That descriptionsAsList returns translations when origin-step description is None") {
    val descriptions = importService.descriptionAsList(
      Some(stepWithDescriptionAndLanguage(None, "nb")),
      List(
        stepWithDescriptionAndLanguage(Some("Beskrivelse2"), "nn"),
        stepWithDescriptionAndLanguage(Some("Beskrivelse3"), "en")
      )
    )

    descriptions.size should be(2)
    descriptions.map(_.description).mkString(",") should equal("Beskrivelse2,Beskrivelse3")
    descriptions.map(_.language).mkString(",") should equal("nn,en")
  }

  test("That embedUrlsAsList returns origin-step embedUrl and all translations") {
    val embedUrls = importService.embedUrlsAsList(
      stepWithEmbedUrlAndLanguage(Some("http://ndla.no/1"), "nb"),
      List(
        stepWithEmbedUrlAndLanguage(Some("http://ndla.no/2"), "nn"),
        stepWithEmbedUrlAndLanguage(Some("http://ndla.no/3"), "en")
      )
    )

    embedUrls.size should be(3)
    embedUrls.map(_.url).mkString(",") should equal("http://ndla.no/1,http://ndla.no/2,http://ndla.no/3")
    embedUrls.map(_.language).mkString(",") should equal("nb,nn,en")
  }

  test("That embedUrlsAsList returns translations when origin-step description is None") {
    val embedUrls = importService.embedUrlsAsList(
      stepWithEmbedUrlAndLanguage(None, "nb"),
      List(
        stepWithEmbedUrlAndLanguage(Some("http://ndla.no/2"), "nn"),
        stepWithEmbedUrlAndLanguage(Some("http://ndla.no/3"), "en")
      )
    )

    embedUrls.size should be(2)
    embedUrls.map(_.url).mkString(",") should equal("http://ndla.no/2,http://ndla.no/3")
    embedUrls.map(_.language).mkString(",") should equal("nn,en")
  }

  test("That importNode inserts for a new node") {
    val mainImport = MainPackageImport(packageWithNodeId(1), Seq())
    val taxonomyResource = TaxonomyResource("urn:resource:1:123", "test", None, "/urn:topic/urn:resource:1:123")

    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[String])).thenReturn(None)

    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))

    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))
    when(taxononyApiClient.updateResource(any[TaxonomyResource]))
      .thenReturn(Success(taxonomyResource))
    when(migrationApiClient.getAllNodeIds).thenReturn(Memoize(memoizeFunc))

    importService.convert("1", mainImport, CLIENT_ID)

    verify(articleImportClient, times(1)).importArticle(nodeId)
    verify(taxononyApiClient, times(1)).updateResource(any[TaxonomyResource])
  }

  test("That importNode updates for an existing node") {
    val mainImport = MainPackageImport(packageWithNodeId(1), Seq())
    val sanders = Author("author", "Crazy Bernie")
    val license = "pd"
    val copyright = Copyright(license, List(sanders))
    val existingLearningPath = LearningPath(
      Some(1),
      Some(1),
      Some("1"),
      None,
      List(),
      List(),
      None,
      Some(1),
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.CREATED_BY_NDLA,
      new Date(),
      List(),
      "",
      copyright
    )
    val taxonomyResource = TaxonomyResource("urn:resource:1:123", "test", None, "/urn:topic/urn:resource:1:123")

    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[String]))
      .thenReturn(Some(existingLearningPath))
    when(
      learningPathRepository.learningStepWithExternalIdAndForLearningPath(any[Option[String]], any[Option[Long]])(
        any[DBSession])).thenReturn(None)
    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))

    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))
    when(taxononyApiClient.updateResource(any[TaxonomyResource]))
      .thenReturn(Success(taxonomyResource))

    when(migrationApiClient.getAllNodeIds)
      .thenReturn(Memoize[String, Set[ArticleMigrationContent]](memoizeFunc))
    val res = importService.convert("1", mainImport, CLIENT_ID)
    res.isSuccess should be(true)

    res.get.learningsteps.head.embedUrl should equal(
      Seq(EmbedUrl(s"/nb/subjects${taxonomyResource.path}", "nb", EmbedType.OEmbed)))

    verify(articleImportClient, times(1)).importArticle(nodeId)
    verify(taxononyApiClient, times(1)).updateResource(any[TaxonomyResource])
  }

  test("That importNode fails if an article cannot be imported") {
    val mainImport = MainPackageImport(packageWithNodeId(1), Seq())
    val sanders = Author("author", "Crazy Bernie")
    val license = "pd"
    val copyright = Copyright(license, List(sanders))
    val existingLearningPath = LearningPath(
      Some(1),
      Some(1),
      Some("1"),
      None,
      List(),
      List(),
      None,
      Some(1),
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.CREATED_BY_NDLA,
      new Date(),
      List(),
      "",
      copyright
    )
    val taxonomyResource = TaxonomyResource("urn:resource:1:123", "test", None, "/urn:topic/urn:resource:1:123")

    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[String]))
      .thenReturn(Some(existingLearningPath))
    when(
      learningPathRepository.learningStepWithExternalIdAndForLearningPath(any[Option[String]], any[Option[Long]])(
        any[DBSession])).thenReturn(None)

    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))

    when(migrationApiClient.getAllNodeIds)
      .thenReturn(Memoize[String, Set[ArticleMigrationContent]](memoizeFunc))
    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Failure(new HttpRequestException("Received error 422. H5P is not imported to new service", None)))

    val Failure(res) = importService.convert("1", mainImport, CLIENT_ID)
    res.getMessage.contains("Received error 422. H5P is not imported to new service") should be(true)

    verify(learningPathRepository, times(0)).update(any[LearningPath])
    verify(articleImportClient, times(1)).importArticle(nodeId)
  }

  test("That importNode falls back on direct article link if taxonomy lookup fails") {
    val mainImport = MainPackageImport(packageWithNodeId(1), Seq())
    val sanders = Author("author", "Crazy Bernie")
    val license = "pd"
    val copyright = Copyright(license, List(sanders))
    val existingLearningPath = LearningPath(
      Some(1),
      Some(1),
      Some("1"),
      None,
      List(),
      List(),
      None,
      Some(1),
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.CREATED_BY_NDLA,
      new Date(),
      List(),
      "",
      copyright
    )
    val taxonomyResource = TaxonomyResource("urn:resource:1:123", "test", None, "/urn:topic/urn:resource:1:123")

    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[String]))
      .thenReturn(Some(existingLearningPath))
    when(
      learningPathRepository.learningStepWithExternalIdAndForLearningPath(any[Option[String]], any[Option[Long]])(
        any[DBSession])).thenReturn(None)
    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))

    when(migrationApiClient.getAllNodeIds)
      .thenReturn(Memoize[String, Set[ArticleMigrationContent]](memoizeFunc))
    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Failure(new HttpRequestException("Received error 404 when looking up resource")))
    when(migrationApiClient.getAllNodeIds)
      .thenReturn(Memoize[String, Set[ArticleMigrationContent]](memoizeFunc))

    val Success(res) = importService.convert("1", mainImport, CLIENT_ID)
    res.learningsteps.head.embedUrl should equal(Seq(EmbedUrl("/nb/article/1", "nb", EmbedType.OEmbed)))
  }

  test("That duration is calculated correctly") {
    val pakke = MainPackageImport(packageWithNodeId(1).copy(durationHours = 1, durationMinutes = 1), Seq())
    when(keywordsService.forNodeId(any[Long])).thenReturn(Seq.empty)
    val learningPath = importService.asLearningPath(pakke, None, CLIENT_ID)

    learningPath.duration should be(Some(61))

    val pakke2 = MainPackageImport(packageWithNodeId(1).copy(durationHours = 0, durationMinutes = 0), Seq())
    when(keywordsService.forNodeId(any[Long])).thenReturn(Seq.empty)
    val learningPath2 = importService.asLearningPath(pakke2, None, CLIENT_ID)

    learningPath2.duration should be(Some(1))
  }

  test("That oldToNewLicenseKey throws on invalid license") {
    assertThrows[ImportException] {
      importService.oldToNewLicenseKey("publicdomain")
    }
  }

  test("That oldToNewLicenseKey converts correctly") {
    importService.oldToNewLicenseKey("nolaw").get.license should be(CC0)
    importService.oldToNewLicenseKey("noc").get.license should be(PublicDomain)
  }

  test("That oldToNewLicenseKey does not convert an license that should not be converted") {
    importService.oldToNewLicenseKey("by-sa").get.license should be(CC_BY_SA)
  }

  test("upload should import all articles handle taxonomy with different translations") {
    val mainImport = MainPackageImport(packageWithNodeId(1), Seq())
    val sanders = Author("author", "Crazy Bernie")
    val license = "pd"
    val copyright = Copyright(license, List(sanders))
    val existingLearningPath = LearningPath(
      Some(1),
      Some(1),
      Some("1"),
      None,
      List(),
      List(),
      None,
      Some(1),
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.CREATED_BY_NDLA,
      new Date(),
      List(),
      "",
      copyright
    )
    val taxonomyResource = TaxonomyResource("urn:resource:1:123", "test", None, "/urn:topic/urn:resource:1:123")

    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[String]))
      .thenReturn(Some(existingLearningPath))
    when(
      learningPathRepository.learningStepWithExternalIdAndForLearningPath(any[Option[String]], any[Option[Long]])(
        any[DBSession])).thenReturn(None)
    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))

    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))
    when(taxononyApiClient.updateResource(any[TaxonomyResource]))
      .thenReturn(Success(taxonomyResource))

    val articleNids = Set(ArticleMigrationContent("54321", "54321"), ArticleMigrationContent("00987", "54321"))
    when(migrationApiClient.getAllNodeIds).thenReturn(Memoize[String, Set[ArticleMigrationContent]]((_: String) =>
      articleNids))

    importService.convert("1", mainImport, CLIENT_ID)

    verify(taxononyApiClient, times(1)).getResource(articleNids.head.nid)
    verify(taxononyApiClient, times(1)).getResource(articleNids.last.nid)

    verify(articleImportClient, times(1)).importArticle(articleNids.head.nid)
    verify(taxononyApiClient, times(1)).updateResource(any[TaxonomyResource])
  }

  test("previously imported learningpaths should be deleted if failed to re-import") {
    val mainImport = MainPackageImport(packageWithNodeId(1), Seq())
    val sanders = Author("author", "Crazy Bernie")
    val license = "pd"
    val copyright = Copyright(license, List(sanders))
    val learningStep = LearningStep(Some(234),
                                    Some(1),
                                    None,
                                    Some(1),
                                    1,
                                    Seq(Title("lp", "nb")),
                                    Seq(Description("desc", "nb")),
                                    Seq.empty,
                                    StepType.TEXT,
                                    None,
                                    true)
    val existingLearningPath = LearningPath(
      Some(1),
      Some(1),
      Some("1"),
      None,
      List(),
      List(),
      None,
      Some(1),
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.CREATED_BY_NDLA,
      new Date(),
      List(),
      "",
      copyright,
      Seq(learningStep)
    )
    val taxonomyResource = TaxonomyResource("urn:resource:1:123", "test", None, "/urn:topic/urn:resource:1:123")

    when(keywordsService.forNodeId(any[Long])).thenReturn(List())
    when(learningPathRepository.withExternalId(any[String]))
      .thenReturn(Some(existingLearningPath))
    when(
      learningPathRepository.learningStepWithExternalIdAndForLearningPath(any[Option[String]], any[Option[Long]])(
        any[DBSession])).thenReturn(None)
    when(learningPathRepository.getIdFromExternalId(any[String])(any[DBSession]))
      .thenReturn(Some(1: Long))

    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Success(ArticleImportStatus(Seq.empty, Seq.empty, 1)))
    when(taxononyApiClient.getResource(any[String]))
      .thenReturn(Success(taxonomyResource))

    when(migrationApiClient.getAllNodeIds)
      .thenReturn(Memoize[String, Set[ArticleMigrationContent]](memoizeFunc))
    when(migrationApiClient.getLearningPath("1"))
      .thenReturn(Success(mainImport))
    when(articleImportClient.importArticle(any[String]))
      .thenReturn(Failure(new HttpRequestException("Received error 422. H5P is not imported to new service", None)))

    val Failure(res) = importService.doImport("1", CLIENT_ID)
    res.getMessage.contains("Received error 422. H5P is not imported to new service") should be(true)

    verify(learningPathRepository, times(0)).update(any[LearningPath])
    verify(learningPathRepository, times(1)).deletePath(1)
    verify(learningPathRepository, times(1)).deleteStep(234)
    verify(articleImportClient, times(1)).importArticle(nodeId)
  }

  test("That empty licenses are turned to None") {
    val pakke = MainPackageImport(packageWithNodeId(1).copy(
                                    steps = Seq(stepWithDescriptionAndLanguage(Some("Heisann"), "nb")
                                      .copy(license = Some("")))),
                                  Seq())
    when(keywordsService.forNodeId(any[Long])).thenReturn(Seq.empty)
    val learningPath = importService.asLearningPath(pakke, None, CLIENT_ID)

    learningPath.learningsteps.head.license should be(None)
  }

  test("That imported learningPaths only include tags in relevant languages") {
    val pack1 = packageWithNodeId(1)
    val pack2 = packageWithNodeId(2).copy(tnid = 1, language = "en")

    val nbTags = LearningPathTags(Seq("hei", "norge", "knekkebrød"), "nb")
    val nbTags2 = LearningPathTags(Seq("brunost", "også"), "nb")
    val nbMerged = LearningPathTags(nbTags.tags ++ nbTags2.tags, "nb")

    val enTags = LearningPathTags(Seq("hello", "englang", "chips"), "en")
    val zhTags = LearningPathTags(Seq("我不懂中文", "亨里克"), "zh")
    val pakke = MainPackageImport(pack1, Seq(pack2))

    when(keywordsService.forNodeId(any[Long])).thenReturn(Seq(nbTags, enTags, zhTags)).thenReturn(Seq(nbTags2))

    val learningPath = importService.asLearningPath(pakke, None, CLIENT_ID)

    learningPath.tags should be(Seq(enTags, nbMerged))
  }

  private def packageWithNodeId(nid: Long): Package =
    Package(
      nid,
      nid,
      "nb",
      "NodeTitle",
      None,
      "NodeDescription",
      1,
      new Date(),
      1,
      "PackageTittel",
      1,
      1,
      Seq(stepWithEmbedUrlAndLanguage(Some("http://ndla.no/node/12345"), "nb")),
      "by-sa",
      Seq(MigrationAuthor("Redaksjonelt", "Henrik"))
    )
  private def stepWithDescriptionAndLanguage(description: Option[String], language: String): Step =
    Step(1, 1, 1, "Tittel", 1, 1, None, description, None, language)
  private def stepWithEmbedUrlAndLanguage(embedUrl: Option[String], language: String): Step =
    Step(1, 1, 1, "Tittel", 1, 1, embedUrl, None, None, language)
}
