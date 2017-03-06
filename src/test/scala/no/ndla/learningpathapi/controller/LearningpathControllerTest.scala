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

import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain.{AccessDeniedException, Sort, ValidationException}
import no.ndla.learningpathapi.{LearningpathApiProperties, LearningpathSwagger, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.getLicenses
import org.json4s.native.Serialization._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

class LearningpathControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new LearningpathSwagger

  val copyright = Copyright(License("by-sa", None, None), List())
  val DefaultLearningPathSummary = LearningPathSummary(1, List(Title("Tittel", Some("nb"))), List(), List(), "", None, None, "", new Date(), List(), copyright, None)

  lazy val controller = new LearningpathController
  addServlet(controller, "/*")

  override def beforeEach() = resetMocks()

  test("That requireHeader returns header value when header exists") {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader("username")).thenReturn("verdi")
    assertResult(Some("verdi")) {
      controller.requireHeader("username")
    }
  }

  test("That requireHeader throws HeaderMissingException if heades does not exist") {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader("username")).thenReturn(null)
    when(request.getRequestURI).thenReturn(""
    )
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { controller.requireHeader("username") }.getMessage
    }
  }

  test("That GET / will send all query-params to the search service") {
    val query = "hoppetau"
    val tag = "lek"
    val language = "nb"
    val page = 22
    val pageSize = 111
    val ids = "1,2"

    val searchResult = SearchResult(1, page, pageSize, Seq(DefaultLearningPathSummary))
    when(searchService.matchingQuery(eqTo(List(1,2)), eqTo(Seq(query)), eqTo(Some(tag)), eqTo(Some(language)), eqTo(Sort.ByDurationDesc), eqTo(Some(page)), eqTo(Some(pageSize)))).thenReturn(searchResult)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)
    get("/", Map(
      "query" -> query,
      "tag" -> tag,
      "language" -> language,
      "sort" -> "-duration",
      "page-size" -> s"$pageSize",
      "page" -> s"$page",
      "ids" -> s"$ids"
    )) {
      status should equal (200)
      val convertedBody = read[SearchResult](body)
      convertedBody.results.head.title.head.title should equal ("Tittel")
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

    when(searchService.all(eqTo(List(1,2)), eqTo(None), eqTo(Sort.ByTitleAsc), eqTo(None), eqTo(None), eqTo(None))).thenReturn(SearchResult(-1, 1, 1, List()))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    get("/", Map(
      "query" -> query,
      "tag" -> tag,
      "language" -> language,
      "sort" -> duration,
      "page-size" -> s"$pageSize",
      "page" -> s"$page",
      "ids" -> s"$ids"
    )) {
      status should equal (200)
      val convertedBody = read[SearchResult](body)
      convertedBody.totalCount should be (-1)
    }

  }

  test ("That GET /licenses with filter sat to by only returns creative common licenses") {
    val creativeCommonlicenses = getLicenses.filter(_.license.startsWith("by")).map(l => License(l.license, Option(l.description), l.url)).toSet

    get("/licenses", Map(
      "filter" -> "by"
    )) {
      status should equal (200)
      val convertedBody = read[Set[License]](body)
      convertedBody should equal(creativeCommonlicenses)
    }
  }

  test ("That GET /licenses with filter not specified returns all licenses") {
    val allLicenses = getLicenses.map(l => License(l.license, Option(l.description), l.url)).toSet

    get("/licenses", Map()) {
      status should equal (200)
      val convertedBody = read[Set[License]](body)
      convertedBody should equal(allLicenses)
    }
  }

  test("That paramAsListOfLong returns empty list when empty param") {
    import scala.collection.JavaConverters._
    implicit val request = mock[HttpServletRequest]
    val paramName = "test"
    val parameterMap = Map("someOther" -> Array(""))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)
    controller.paramAsListOfLong(paramName)(request) should equal (List())
  }

  test("That paramAsListOfLong returns List of longs for all ids specified in input") {
    import scala.collection.JavaConverters._
    implicit val request = mock[HttpServletRequest]
    val expectedList = List(1,2,3,5,6,7,8)
    val paramName = "test"
    val parameterMap = Map(paramName -> Array(expectedList.mkString(" , ")))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)
    controller.paramAsListOfLong(paramName)(request) should equal (expectedList)
  }

  test("That paramAsListOfLong returns validation error when list of ids contains a string") {
    import scala.collection.JavaConverters._
    implicit val request = mock[HttpServletRequest]
    val paramName = "test"
    val parameterMap = Map(paramName -> Array("1,2,abc,3"))

    when(request.getParameterMap).thenReturn(parameterMap.asJava)

    val validationException = intercept[ValidationException]{
      controller.paramAsListOfLong(paramName)(request)
    }

    validationException.errors.size should be (1)
    validationException.errors.head.field should equal (paramName)
    validationException.errors.head.message should equal (s"Invalid value for $paramName. Only (list of) digits are allowed.")

  }
}
