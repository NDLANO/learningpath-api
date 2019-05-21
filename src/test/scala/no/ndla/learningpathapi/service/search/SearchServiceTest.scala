/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import java.nio.file.{Files, Path}

import com.sksamuel.elastic4s.embedded.{InternalLocalNode, LocalNode}
import no.ndla.learningpathapi.LearningpathApiProperties.{DefaultPageSize, MaxPageSize}
import no.ndla.learningpathapi.integration.NdlaE4sClient
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{LearningpathApiProperties, TestEnvironment, UnitSuite}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._

import scala.util.Success

class SearchServiceTest extends UnitSuite with TestEnvironment {
  val tmpDir: Path = Files.createTempDirectory(this.getClass.getName)
  val localNodeSettings: Map[String, String] = LocalNode.requiredSettings(this.getClass.getName, tmpDir.toString)
  val localNode: InternalLocalNode = LocalNode(localNodeSettings)
  override val e4sClient: NdlaE4sClient = NdlaE4sClient(localNode.client(true))

  override val searchConverterService: SearchConverterService =
    new SearchConverterService
  override val searchIndexService: SearchIndexService = new SearchIndexService
  override val searchService: SearchService = new SearchService

  val paul = Author("author", "Truly Weird Rand Paul")
  val license = "publicdomain"
  val copyright = Copyright(license, List(paul))

  val DefaultLearningPath = LearningPath(
    id = None,
    revision = None,
    externalId = None,
    isBasedOn = None,
    title = List(),
    description = List(),
    coverPhotoId = None,
    duration = Some(0),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    lastUpdated = clock.now(),
    tags = List(),
    owner = "owner",
    copyright = copyright
  )

  val PenguinId = 1
  val BatmanId = 2
  val DonaldId = 3
  val UnrelatedId = 4
  val EnglandoId = 5

  override def beforeAll(): Unit = {
    searchIndexService.createIndexWithName(LearningpathApiProperties.SearchIndex)

    doReturn(api.Author("Forfatter", "En eier"), Nil: _*).when(converterService).asAuthor(any[NdlaUserName])

    val today = new DateTime().toDate
    val yesterday = new DateTime().minusDays(1).toDate
    val tomorrow = new DateTime().plusDays(1).toDate
    val tomorrowp1 = new DateTime().plusDays(2).toDate
    val tomorrowp2 = new DateTime().plusDays(3).toDate

    val thePenguin = DefaultLearningPath.copy(
      id = Some(PenguinId),
      title = List(Title("Pingvinen er en kjeltring", "nb")),
      description = List(Description("Dette handler om fugler", "nb")),
      duration = Some(1),
      lastUpdated = yesterday,
      tags = List(LearningPathTags(Seq("superhelt", "kanikkefly"), "nb"))
    )

    val batman = DefaultLearningPath.copy(
      id = Some(BatmanId),
      title = List(Title("Batman er en tøff og morsom helt", "nb"), Title("Batman is a tough guy", "en")),
      description = List(Description("Dette handler om flaggermus, som kan ligne litt på en fugl", "nb")),
      duration = Some(2),
      lastUpdated = today,
      tags = List(LearningPathTags(Seq("superhelt", "kanfly"), "nb"))
    )

    val theDuck = DefaultLearningPath.copy(
      id = Some(DonaldId),
      title = List(Title("Donald er en tøff, rar og morsom and", "nb"), Title("Donald is a weird duck", "en")),
      description = List(Description("Dette handler om en and, som også minner om både flaggermus og fugler.", "nb")),
      duration = Some(3),
      lastUpdated = tomorrow,
      tags = List(LearningPathTags(Seq("disney", "kanfly"), "nb")),
      verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA
    )

    val unrelated = DefaultLearningPath.copy(
      id = Some(UnrelatedId),
      title = List(Title("Unrelated", "en"), Title("Urelatert", "nb")),
      description = List(Description("This is unrelated", "en"), Description("Dette er en urelatert", "nb")),
      duration = Some(4),
      lastUpdated = tomorrowp1,
      tags = List()
    )

    val englando = DefaultLearningPath.copy(
      id = Some(EnglandoId),
      title = List(Title("Englando", "en")),
      description = List(Description("This is a englando learningpath", "en")),
      duration = Some(5),
      lastUpdated = tomorrowp2,
      tags = List()
    )

    searchIndexService.indexDocument(thePenguin)
    searchIndexService.indexDocument(batman)
    searchIndexService.indexDocument(theDuck)
    searchIndexService.indexDocument(unrelated)
    searchIndexService.indexDocument(englando)

    blockUntil(() => searchService.countDocuments() == 5)
  }

  override def afterAll(): Unit = {
    searchIndexService.deleteIndexWithName(Some(LearningpathApiProperties.SearchIndex))
  }

  test("all learningpaths should be returned if fallback is enabled in all-search") {
    val Success(res) =
      searchService.allV2(List.empty, None, Sort.ByIdDesc, "hurr durr I'm a language", Some(1), None, fallback = true)
    res.results.length should be(res.totalCount)
    res.totalCount should be(5)
  }

  test("no learningpaths should be returned if fallback is disabled with an unsupported language in all-search") {
    val Success(res) =
      searchService.allV2(List.empty, None, Sort.ByIdDesc, "hurr durr I'm a language", Some(1), None, fallback = false)
    res.results.length should be(res.totalCount)
    res.totalCount should be(0)
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService.getStartAtAndNumResults(None, None) should equal((0, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(None, Some(1000)) should equal((0, MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    searchService.getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 100
    val pageSize = 10
    val expectedStartAt = (page - 1) * pageSize
    searchService.getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }

  test("That all learningpaths are returned ordered by title descending") {
    val Success(searchResult) = searchService.allV2(List(), None, Sort.ByTitleDesc, "nb", None, None, fallback = false)
    val hits = searchResult.results
    searchResult.totalCount should be(4)

    hits.head.id should be(UnrelatedId)
    hits(1).id should be(PenguinId)
    hits(2).id should be(DonaldId)
    hits(3).id should be(BatmanId)

  }

  test("That all learningpaths are returned ordered by title ascending") {
    val Success(searchResult) =
      searchService.allV2(List(), None, Sort.ByTitleAsc, "nb", None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(BatmanId)
    hits(1).id should be(DonaldId)
    hits(2).id should be(PenguinId)
    hits(3).id should be(UnrelatedId)
  }

  test("That all learningpaths are returned ordered by id descending") {
    val Success(searchResult) =
      searchService.allV2(List(), None, Sort.ByIdDesc, "nb", None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(UnrelatedId)
    hits(1).id should be(DonaldId)
    hits(2).id should be(BatmanId)
    hits(3).id should be(PenguinId)
  }

  test("That all learningpaths are returned ordered by id ascending") {
    val Success(searchResult) =
      searchService.allV2(List(), None, Sort.ByIdAsc, "all", None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(5)
    hits.head.id should be(PenguinId)
    hits(1).id should be(BatmanId)
    hits(2).id should be(DonaldId)
    hits(3).id should be(UnrelatedId)
    hits(4).id should be(EnglandoId)
  }

  test("That order by durationDesc orders search result by duration descending") {
    val Success(searchResult) =
      searchService.allV2(List(), None, Sort.ByDurationDesc, "nb", None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(UnrelatedId)
  }

  test("That order ByDurationAsc orders search result by duration ascending") {
    val Success(searchResult) =
      searchService.allV2(List(), None, Sort.ByDurationAsc, "nb", None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(PenguinId)
  }

  test("That order ByLastUpdatedDesc orders search result by last updated date descending") {
    val Success(searchResult) =
      searchService.allV2(List(), None, Sort.ByLastUpdatedDesc, "nb", None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(UnrelatedId)
    hits.last.id should be(PenguinId)
  }

  test("That order ByLastUpdatedAsc orders search result by last updated date ascending") {
    val Success(searchResult) =
      searchService.allV2(List(), None, Sort.ByLastUpdatedAsc, "nb", None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(4)
    hits.head.id should be(PenguinId)
    hits.last.id should be(UnrelatedId)
  }

  test("That all filtered by id only returns learningpaths with the given ids") {
    val Success(searchResult) =
      searchService.allV2(List(1, 2), None, Sort.ByTitleAsc, Language.AllLanguages, None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(BatmanId)
    hits.last.id should be(PenguinId)
  }

  test("That searching returns matching documents with unmatching language if fallback is enabled") {
    val Success(searchResult) =
      searchService.matchingQuery(List(), "Pingvinen", None, "en", Sort.ByTitleAsc, None, None, fallback = true, None)

    searchResult.totalCount should be(1)
  }

  test("That searching returns no matching documents with unmatching language if fallback is disabled ") {
    val Success(searchResult) =
      searchService.matchingQuery(List(), "Pingvinen", None, "en", Sort.ByTitleAsc, None, None, fallback = false, None)

    searchResult.totalCount should be(0)
  }

  test("That searching only returns documents matching the query") {
    val Success(searchResult) =
      searchService.matchingQuery(List(), "heltene", None, "nb", Sort.ByTitleAsc, None, None, fallback = false, None)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val Success(searchResult) =
      searchService.matchingQuery(List(3),
                                  "morsom",
                                  None,
                                  Language.AllLanguages,
                                  Sort.ByTitleAsc,
                                  None,
                                  None,
                                  fallback = false,
                                  None)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That searching only returns documents matching the query in the specified language") {
    val Success(searchResult) =
      searchService.matchingQuery(List(), "guy", None, "en", Sort.ByTitleAsc, None, None, fallback = false, None)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That filtering on tag only returns documents where the tag is present") {
    val Success(searchResult) =
      searchService.allV2(List(), Some("superhelt"), Sort.ByTitleAsc, "nb", None, None, fallback = false)
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(BatmanId)
    hits.last.id should be(PenguinId)
  }

  test(
    "That filtering on tag combined with search only returns documents where the tag is present and the search matches the query") {
    val Success(searchResult) =
      searchService.matchingQuery(List(),
                                  "heltene",
                                  Some("kanfly"),
                                  "nb",
                                  Sort.ByTitleAsc,
                                  None,
                                  None,
                                  fallback = false,
                                  None)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That searching and ordering by relevance is returning Donald before Batman when searching for tough weirdos") {
    val Success(searchResult) =
      searchService.matchingQuery(List(),
                                  "tøff rar",
                                  None,
                                  "nb",
                                  Sort.ByRelevanceDesc,
                                  None,
                                  None,
                                  fallback = false,
                                  None)
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test(
    "That searching and ordering by relevance is returning Donald before Batman and the penguin when searching for duck, bat and bird") {
    val Success(searchResult) =
      searchService.matchingQuery(List(),
                                  "and flaggermus fugl",
                                  None,
                                  "nb",
                                  Sort.ByRelevanceDesc,
                                  None,
                                  None,
                                  fallback = false,
                                  None)
    val hits = searchResult.results

    searchResult.totalCount should be(3)
    hits.toList.head.id should be(DonaldId)
    hits.toList(1).id should be(BatmanId)
    hits.toList(2).id should be(PenguinId)
  }

  test(
    "That searching and ordering by relevance is not returning Penguin when searching for duck, bat and bird, but filtering on kanfly") {
    val Success(searchResult) = searchService.matchingQuery(List(),
                                                            "and flaggermus fugl",
                                                            Some("kanfly"),
                                                            "nb",
                                                            Sort.ByRelevanceDesc,
                                                            None,
                                                            None,
                                                            fallback = false,
                                                            None)
    val hits = searchResult.results

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test("That a search for flaggremsu returns Donald but not Batman if it is misspelled") {
    val Success(searchResult) =
      searchService.matchingQuery(List(),
                                  "and flaggremsu",
                                  None,
                                  "nb",
                                  Sort.ByRelevanceDesc,
                                  None,
                                  None,
                                  fallback = false,
                                  None)
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That searching with logical operators works") {
    val Success(searchResult1) =
      searchService.matchingQuery(List(),
                                  "kjeltring + batman",
                                  None,
                                  "nb",
                                  Sort.ByRelevanceAsc,
                                  None,
                                  None,
                                  fallback = false,
                                  None)
    searchResult1.totalCount should be(0)

    val Success(searchResult2) =
      searchService.matchingQuery(List(),
                                  "tøff + morsom + -and",
                                  None,
                                  "nb",
                                  Sort.ByRelevanceAsc,
                                  None,
                                  None,
                                  fallback = false,
                                  None)
    val hits2 = searchResult2.results

    searchResult2.totalCount should be(1)
    hits2.head.id should be(BatmanId)

    val Success(searchResult3) = searchService.matchingQuery(List(),
                                                             "tøff | morsom | kjeltring",
                                                             None,
                                                             "nb",
                                                             Sort.ByRelevanceAsc,
                                                             None,
                                                             None,
                                                             fallback = false,
                                                             None)
    val hits3 = searchResult3.results

    searchResult3.totalCount should be(3)
    hits3.head.id should be(PenguinId)
    hits3(1).id should be(DonaldId)
    hits3.last.id should be(BatmanId)
  }

  test("That searching for multiple languages returns result in matched language") {
    val Success(searchNb) =
      searchService.matchingQuery(List(), "Urelatert", None, "all", Sort.ByTitleAsc, None, None, fallback = false, None)
    val Success(searchEn) =
      searchService.matchingQuery(List(), "Unrelated", None, "all", Sort.ByTitleAsc, None, None, fallback = false, None)

    searchEn.totalCount should be(1)
    searchEn.results.head.id should be(UnrelatedId)
    searchEn.results.head.title.language should be("en")
    searchEn.results.head.title.title should be("Unrelated")
    searchEn.results.head.description.description should be("This is unrelated")
    searchEn.results.head.description.language should be("en")

    searchNb.totalCount should be(1)
    searchNb.results.head.id should be(UnrelatedId)
    searchNb.results.head.title.language should be("nb")
    searchNb.results.head.title.title should be("Urelatert")
    searchNb.results.head.description.description should be("Dette er en urelatert")
    searchNb.results.head.description.language should be("nb")
  }

  test("That searching for all languages returns multiple languages") {
    val Success(search) = searchService.allV2(List(), None, Sort.ByTitleAsc, "all", None, None, fallback = false)

    search.totalCount should be(5)
    search.results.head.id should be(BatmanId)
    search.results(1).id should be(DonaldId)
    search.results(2).id should be(EnglandoId)
    search.results(2).title.language should be("en")
    search.results(3).id should be(PenguinId)
    search.results(4).id should be(UnrelatedId)
    search.results(4).title.language should be("nb")
  }

  test("that supportedLanguages are sorted correctly") {
    val Success(search) =
      searchService.matchingQuery(List(), "Batman", None, "all", Sort.ByTitleAsc, None, None, fallback = false, None)
    search.results.head.supportedLanguages should be(Seq("nb", "en"))
  }

  test("That searching with fallback still returns searched language if specified") {
    val Success(search) = searchService.allV2(List(), None, Sort.ByIdAsc, "en", None, None, fallback = true)

    search.totalCount should be(5)
    search.results.head.id should be(PenguinId)
    search.results(1).id should be(BatmanId)
    search.results(2).id should be(DonaldId)
    search.results(3).id should be(UnrelatedId)
    search.results(4).id should be(EnglandoId)

    search.results.map(_.id) should be(Seq(1, 2, 3, 4, 5))
    search.results.map(_.title.language) should be(Seq("nb", "en", "en", "en", "en"))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val expectedIds = List(1, 2, 3, 4, 5).sliding(pageSize, pageSize).toList

    val Success(initialSearch) =
      searchService.allV2(List(), None, Sort.ByIdAsc, "all", None, Some(pageSize), fallback = true)

    val Success(scroll1) = searchService.scroll(initialSearch.scrollId.get, "all")
    val Success(scroll2) = searchService.scroll(scroll1.scrollId.get, "all")
    val Success(scroll3) = searchService.scroll(scroll2.scrollId.get, "all")

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(List.empty)
  }

  test("That search combined with filter by verification status only returns documents matching the query with the given verification status") {
    val Success(searchResult) =
      searchService.matchingQuery(List(),
        "flaggermus",
        None,
        Language.AllLanguages,
        Sort.ByTitleAsc,
        None,
        None,
        fallback = false,
        Some("EXTERNAL"))
    val hits = searchResult.results

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  def blockUntil(predicate: () => Boolean): Unit = {
    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable => println("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting for predicate")
  }
}
