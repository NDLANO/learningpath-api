/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import java.util.Date
import javax.servlet.http.HttpServletRequest

import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.api.SearchResultV2
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{LearningpathSwagger, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.getLicenses
import org.json4s.native.Serialization._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

class LearningpathControllerTestV2 extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new LearningpathSwagger

  val copyright = api.Copyright(api.License("by-sa", None, None), List())

  val DefaultLearningPathSummary = api.LearningPathSummaryV2(
    1,
    api.Title("Tittel", "nb"),
    api.Description("", "nb"),
    api.Introduction("", "nb"),
    "",
    None,
    None,
    "",
    new Date(),
    api.LearningPathTags(Seq(), "nb"),
    copyright,
    List("nb"),
    None
  )

  lazy val controller = new LearningpathControllerV2
  addServlet(controller, "/*")

  override def beforeEach() = {
    resetMocks()
    when(languageValidator.validate(any[String], any[String], any[Boolean]))
      .thenReturn(None)
  }

  test("That GET / will send all query-params to the search service") {
    val query = "hoppetau"
    val tag = "lek"
    val language = "nb"
    val page = 22
    val pageSize = 111
    val ids = "1,2"

    val result = SearchResultV2(1, 1, 1, "nb", Seq(DefaultLearningPathSummary))

    when(
      searchService.matchingQuery(eqTo(List(1, 2)),
                                  eqTo(query),
                                  eqTo(Some(tag)),
                                  eqTo(language),
                                  eqTo(Sort.ByDurationDesc),
                                  eqTo(Some(page)),
                                  eqTo(Some(pageSize)),
                                  eqTo(false))).thenReturn(result)
    //when(searchService.getHitsV2(searchResult, language)).thenReturn(Seq(DefaultLearningPathSummary))

    get("/",
        Map(
          "query" -> query,
          "tag" -> tag,
          "language" -> language,
          "sort" -> "-duration",
          "page-size" -> s"$pageSize",
          "page" -> s"$page",
          "ids" -> s"$ids"
        )) {
      status should equal(200)
      val convertedBody = read[api.SearchResultV2](body)
      convertedBody.results.head.title should equal(api.Title("Tittel", "nb"))
    }
  }

  test("That GET / will handle all empty query-params as missing query params") {
    val query = ""
    val tag = ""
    val language = ""
    val page = ""
    val pageSize = ""
    val duration = ""
    val ids = "1,2"

    val result = SearchResultV2(-1, 1, 1, "nb", Seq(DefaultLearningPathSummary))

    when(
      searchService.allV2(any[List[Long]],
                          any[Option[String]],
                          any[Sort.Value],
                          any[String],
                          any[Option[Int]],
                          any[Option[Int]],
                          eqTo(false))).thenReturn(result)

    get("/",
        Map(
          "query" -> query,
          "tag" -> tag,
          "language" -> language,
          "sort" -> duration,
          "page-size" -> s"$pageSize",
          "page" -> s"$page",
          "ids" -> s"$ids"
        )) {
      status should equal(200)
      val convertedBody = read[api.SearchResultV2](body)
      convertedBody.totalCount should be(-1)
    }

  }

  test("That POST /search will send all query-params to the search service") {
    val query = "hoppetau"
    val tag = "lek"
    val language = "nb"
    val page = 22
    val pageSize = 111

    val result = SearchResultV2(1, page, pageSize, language, Seq(DefaultLearningPathSummary))

    when(
      searchService.matchingQuery(eqTo(List(1, 2)),
                                  eqTo(query),
                                  eqTo(Some(tag)),
                                  eqTo(language),
                                  eqTo(Sort.ByDurationDesc),
                                  eqTo(Some(page)),
                                  eqTo(Some(pageSize)),
                                  eqTo(false))).thenReturn(result)

    post(
      "/search/",
      body =
        s"""{"query": "$query", "tag": "$tag", "language": "$language", "page": $page, "pageSize": $pageSize, "ids": [1, 2], "sort": "-duration" }"""
    ) {
      status should equal(200)
      val convertedBody = read[api.SearchResultV2](body)
      convertedBody.results.head.title should equal(api.Title("Tittel", "nb"))
    }
  }

  test("That GET /licenses with filter sat to by only returns creative common licenses") {
    val creativeCommonlicenses = getLicenses
      .filter(_.license.startsWith("by"))
      .map(l => api.License(l.license, Option(l.description), l.url))
      .toSet

    get("/licenses/",
        Map(
          "filter" -> "by"
        )) {
      status should equal(200)
      val convertedBody = read[Set[api.License]](body)
      convertedBody should equal(creativeCommonlicenses)
    }
  }

  test("That GET /licenses with filter not specified returns all licenses") {
    val allLicenses = getLicenses
      .map(l => api.License(l.license, Option(l.description), l.url))
      .toSet

    get("/licenses/", Map()) {
      status should equal(200)
      val convertedBody = read[Set[api.License]](body)
      convertedBody should equal(allLicenses)
    }
  }

  test("That paramAsListOfLong returns empty list when empty param") {
    import scala.collection.JavaConverters._
    implicit val request = mock[HttpServletRequest]
    val paramName = "test"
    val parameterMap = Map("someOther" -> Array(""))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)
    controller.paramAsListOfLong(paramName)(request) should equal(List())
  }

  test("That paramAsListOfLong returns List of longs for all ids specified in input") {
    import scala.collection.JavaConverters._
    implicit val request = mock[HttpServletRequest]
    val expectedList = List(1, 2, 3, 5, 6, 7, 8)
    val paramName = "test"
    val parameterMap = Map(paramName -> Array(expectedList.mkString(" , ")))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)
    controller.paramAsListOfLong(paramName)(request) should equal(expectedList)
  }

  test("That paramAsListOfLong returns validation error when list of ids contains a string") {
    import scala.collection.JavaConverters._
    implicit val request = mock[HttpServletRequest]
    val paramName = "test"
    val parameterMap = Map(paramName -> Array("1,2,abc,3"))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)

    val validationException = intercept[ValidationException] {
      controller.paramAsListOfLong(paramName)(request)
    }

    validationException.errors.size should be(1)
    validationException.errors.head.field should equal(paramName)
    validationException.errors.head.message should equal(
      s"Invalid value for $paramName. Only (list of) digits are allowed.")

  }
}
