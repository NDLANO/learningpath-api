/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search


import no.ndla.learningpathapi.LearningpathApiProperties.{DefaultPageSize, MaxPageSize}
import no.ndla.learningpathapi.integration.JestClientFactory
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{LearningpathApiProperties, TestEnvironment, UnitSuite}
import no.ndla.tag.IntegrationTest
import org.joda.time.DateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._

@IntegrationTest
class SearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esPort")
  override val searchConverterService: SearchConverterService = new SearchConverterService
  override val searchIndexService: SearchIndexService = new SearchIndexService
  override val searchService: SearchService = new SearchService

  val paul = Author("author", "Truly Weird Rand Paul")
  val license = "publicdomain"
  val copyright = Copyright(license, List(paul))
  val DefaultLearningPath = LearningPath(
    id = None,
    revision = None, externalId = None, isBasedOn = None,
    title = List(),
    description = List(),
    coverPhotoId = None,
    duration = Some(0),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    lastUpdated = clock.now(),
    tags = List(),
    owner = "owner",
    copyright = copyright)

  val PenguinId = 1
  val BatmanId = 2
  val DonaldId = 3

  override def beforeAll() = {
    searchIndexService.createIndexWithName(LearningpathApiProperties.SearchIndex)

    doReturn(api.Author("Forfatter", "En eier")).when(converterService).asAuthor(any[NdlaUserName])

    val today = new DateTime().toDate
    val yesterday = new DateTime().minusDays(1).toDate
    val tomorrow = new DateTime().plusDays(1).toDate

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
      title = List(Title("Donald er en tøff, rar og morsom and", "nb"), Title("Donald is a weird duck", "nb")),
      description = List(Description("Dette handler om en and, som også minner om både flaggermus og fugler.", "nb")),
      duration = Some(3),
      lastUpdated = tomorrow,
      tags = List(LearningPathTags(Seq("disney", "kanfly"), "nb"))
    )

    searchIndexService.indexDocument(thePenguin)
    searchIndexService.indexDocument(batman)
    searchIndexService.indexDocument(theDuck)

    blockUntil(() => searchService.countDocuments() == 3)
  }

  override def afterAll() = {
    searchIndexService.delete(Some(LearningpathApiProperties.SearchIndex))
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService.getStartAtAndNumResults(None, None) should equal((0, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(None, Some(1000)) should equal((0, MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
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
    val searchResult = searchService.all(List(), None, Sort.ByTitleDesc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")
    searchResult.totalCount should be(3)

    hits.head.id should be(PenguinId)
  }

  test("That all learningpaths are returned ordered by title ascending") {
    val searchResult = searchService.all(List(), None, Sort.ByTitleAsc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(3)
    hits.head.id should be(BatmanId)
  }

  test("That all learningpaths are returned ordered by id descending") {
    val searchResult = searchService.all(List(), None, Sort.ByIdDesc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(3)
    hits.head.id should be(DonaldId)
    hits.last.id should be(PenguinId)
  }

  test("That all learningpaths are returned ordered by id ascending") {
    val searchResult = searchService.all(List(), None, Sort.ByIdAsc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(3)
    hits.head.id should be(PenguinId)
    hits.last.id should be(DonaldId)
  }

  test("That order by durationDesc orders search result by duration descending") {
    val searchResult = searchService.all(List(), None, Sort.ByDurationDesc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(3)
    hits.head.id should be(DonaldId)
  }

  test("That order ByDurationAsc orders search result by duration ascending") {
    val searchResult = searchService.all(List(), None, Sort.ByDurationAsc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(3)
    hits.head.id should be(PenguinId)
  }

  test("That order ByLastUpdatedDesc orders search result by last updated date descending") {
    val searchResult = searchService.all(List(), None, Sort.ByLastUpdatedDesc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(3)
    hits.head.id should be(DonaldId)
    hits.last.id should be(PenguinId)
  }

  test("That order ByLastUpdatedAsc orders search result by last updated date ascending") {
    val searchResult = searchService.all(List(), None, Sort.ByLastUpdatedAsc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(3)
    hits.head.id should be(PenguinId)
    hits.last.id should be(DonaldId)
  }

  test("That all filtered by id only returns learningpaths with the given ids") {
    val searchResult = searchService.all(List(1, 2), None, Sort.ByTitleAsc, None, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(2)
    hits.head.id should be(BatmanId)
    hits.last.id should be(PenguinId)
  }

  test("That searching only returns documents matching the query") {
    val searchResult = searchService.matchingQuery(List(), "heltene", None, Some("nb"), Sort.ByTitleAsc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That search combined with filter by id only returns documents matching the query with one of the given ids") {
    val searchResult = searchService.matchingQuery(List(3), "morsom", None, None, Sort.ByTitleAsc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That searching only returns documents matching the query in the specified language") {
    val searchResult = searchService.matchingQuery(List(), "guy", None, Some("en"), Sort.ByTitleAsc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That filtering on tag only returns documents where the tag is present") {
    val searchResult = searchService.all(List(), Some("superhelt"), Sort.ByTitleAsc, Some("nb"), None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(2)
    hits.head.id should be(BatmanId)
    hits.last.id should be(PenguinId)
  }

  test("That filtering on tag combined with search only returns documents where the tag is present and the search matches the query") {
    val searchResult = searchService.matchingQuery(List(), "heltene", Some("kanfly"), Some("nb"), Sort.ByTitleAsc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(1)
    hits.head.id should be(BatmanId)
  }

  test("That searching and ordering by relevance is returning Donald before Batman when searching for tough weirdos") {
    val searchResult = searchService.matchingQuery(List(), "tøff rar", None, Some("nb"), Sort.ByRelevanceDesc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test("That searching and ordering by relevance is returning Donald before Batman and the penguin when searching for duck, bat and bird") {
    val searchResult = searchService.matchingQuery(List(), "and flaggermus fugl", None, Some("nb"), Sort.ByRelevanceDesc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(3)
    hits.toList(0).id should be(DonaldId)
    hits.toList(1).id should be(BatmanId)
    hits.toList(2).id should be(PenguinId)
  }

  test("That searching and ordering by relevance is not returning Penguin when searching for duck, bat and bird, but filtering on kanfly") {
    val searchResult = searchService.matchingQuery(List(), "and flaggermus fugl", Some("kanfly"), Some("nb"), Sort.ByRelevanceDesc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test("That a search for flaggremsu returns both Donald and Batman even if it is misspelled") {
    val searchResult = searchService.matchingQuery(List(), "and flaggremsu", None, Some("nb"), Sort.ByRelevanceDesc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(2)
    hits.head.id should be(DonaldId)
    hits.last.id should be(BatmanId)
  }

  test("That a search for flaggremsu returns Donald but not Batman if it is misspelled") {
    val searchResult = searchService.matchingQuery(List(), "and flaggremsu", None, Some("nb"), Sort.ByRelevanceDesc, None, None)
    val hits = searchService.getHitsV2(searchResult.response, "nb")

    searchResult.totalCount should be(1)
    hits.head.id should be(DonaldId)
  }

  test("That searching with logical operators works") {
    val searchResult1 = searchService.matchingQuery(List(), "kjeltring + batman", None, Some("nb"), Sort.ByRelevanceAsc, None, None)
    searchResult1.totalCount should be(0)

    val searchResult2 = searchService.matchingQuery(List(), "tøff + morsom + -and", None, Some("nb"), Sort.ByRelevanceAsc, None, None)
    val hits2 = searchService.getHitsV2(searchResult2.response, "nb")

    searchResult2.totalCount should be(1)
    hits2.head.id should be(BatmanId)

    val searchResult3 = searchService.matchingQuery(List(), "tøff | morsom | kjeltring", None, Some("nb"), Sort.ByRelevanceAsc, None, None)
    val hits3 = searchService.getHitsV2(searchResult3.response, "nb")

    searchResult3.totalCount should be(3)
    hits3.head.id should be(PenguinId)
    hits3(1).id should be(DonaldId)
    hits3.last.id should be(BatmanId)
  }

  def blockUntil(predicate: () => Boolean) = {
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
