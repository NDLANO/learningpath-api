package no.ndla.learningpathapi.service.search

import no.ndla.learningpathapi.{LearningpathApiProperties, TestEnvironment, UnitSuite}

class SearchServiceTest extends UnitSuite with TestEnvironment {

  var service: SearchService = _
  var modelConverter: ConverterService = _

  val DEFAULT_PAGE_SIZE = 12
  val MAX_PAGE_SIZE = 548

  before {
    LearningpathApiProperties.setProperties(Map(
      "SEARCH_MAX_PAGE_SIZE" -> Some(s"$MAX_PAGE_SIZE"),
      "SEARCH_DEFAULT_PAGE_SIZE" -> Some(s"$DEFAULT_PAGE_SIZE")
      ))

    service = new SearchService

  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    service.getStartAtAndNumResults(None, None) should equal((0, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    service.getStartAtAndNumResults(None, Some(1000)) should equal((0, MAX_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DEFAULT_PAGE_SIZE
    service.getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val pageSize = 321
    val expectedStartAt = (page - 1) * pageSize
    service.getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }
}
