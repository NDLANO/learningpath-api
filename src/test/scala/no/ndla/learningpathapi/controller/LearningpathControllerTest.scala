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
import no.ndla.learningpathapi.model.domain.{AccessDeniedException, Sort}
import no.ndla.learningpathapi.{LearningpathApiProperties, LearningpathSwagger, TestEnvironment, UnitSuite}
import org.json4s.native.Serialization._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

class LearningpathControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new LearningpathSwagger

  val copyright = Copyright(License("by-sa", None, None), List())
  val DefaultLearningPathSummary = LearningPathSummary(1, List(Title("Tittel", Some("nb"))), List(), List(), "", None, None, "", new Date(), List(), Author("", ""), copyright)

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

  test("That usernameFromHeader replaces ndla- in the header value") {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader(LearningpathApiProperties.UsernameHeader)).thenReturn("ndla-123-123-123")
    assertResult("123-123-123") {
      controller.usernameFromHeader
    }
  }

  test("That usernameFromHeader does not replace anything else than ndla-") {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader(LearningpathApiProperties.UsernameHeader)).thenReturn("someotherword-123-123-123")
    assertResult("someotherword-123-123-123") {
      controller.usernameFromHeader
    }
  }

  test("That GET / will send all query-params to the search service") {
    val query = "hoppetau"
    val tag = "lek"
    val language = "nb"
    val page = 22
    val pageSize = 111

    val searchResult = SearchResult(1, page, pageSize, Seq(DefaultLearningPathSummary))
    when(searchService.matchingQuery(eqTo(Seq(query)), eqTo(Some(tag)), eqTo(Some(language)), eqTo(Sort.ByDurationDesc), eqTo(Some(page)), eqTo(Some(pageSize)))).thenReturn(searchResult)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)
    get("/", Map(
      "query" -> query,
      "tag" -> tag,
      "language" -> language,
      "sort" -> "-duration",
      "page-size" -> s"$pageSize",
      "page" -> s"$page"
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

    when(searchService.all(eqTo(None), eqTo(Sort.ByTitleAsc), eqTo(None), eqTo(None), eqTo(None))).thenReturn(SearchResult(-1, 1, 1, List()))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    get("/", Map(
      "query" -> query,
      "tag" -> tag,
      "language" -> language,
      "sort" -> duration,
      "page-size" -> s"$pageSize",
      "page" -> s"$page"
    )) {
      status should equal (200)
      val convertedBody = read[SearchResult](body)
      convertedBody.totalCount should be (-1)
    }

  }
}
