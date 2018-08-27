/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito._

import scalaj.http.{HttpRequest, HttpResponse}

class KeywordsServiceTest extends UnitSuite with UnitTestEnvironment {
  val parseableResponse =
    """
      {"keyword": [{
            "names": [{
                "wordclass": "noun",
                "data": [
                  {"http://psi.oasis-open.org/iso/639/#eng": "english poetry"},
                  {"http://psi.oasis-open.org/iso/639/#nob": "bokmålspoesi"},
                  {"http://psi.oasis-open.org/iso/639/#nno": "nynorsk poesi"},
                  {"http://psi.oasis-open.org/iso/639/#language-neutral": "språknøytral"}]}]}]}
    """.stripMargin

  var service: KeywordsService = _

  override def beforeEach() = {
    service = new KeywordsService
    resetMocks()
  }

  test("That forRequest returns empty list when http-error") {
    val resp = mock[HttpResponse[String]]
    when(resp.isError).thenReturn(true)
    when(resp.statusLine).thenReturn("statusline")
    when(resp.code).thenReturn(404)

    service.forRequest(mockRequestWithResponse(resp)) should equal(List())
  }

  test("That forRequest returns empty list for unparsable body") {
    val resp = mock[HttpResponse[String]]
    when(resp.isError).thenReturn(false)
    when(resp.body).thenReturn("adsf")

    service.forRequest(mockRequestWithResponse(resp)) should equal(List())
  }

  test("That forRequest returns list of tags for parseable response") {
    val resp = mock[HttpResponse[String]]
    when(resp.isError).thenReturn(false)

    when(resp.body).thenReturn(parseableResponse)
    val tagsList = service.forRequest(mockRequestWithResponse(resp))
    tagsList.size should be(4)
    tagsList.find(_.language == "unknown").get.tags.size should be(1)
    tagsList.find(_.language == "nb").get.tags.size should be(1)
    tagsList.find(_.language == "nn").get.tags.size should be(1)
    tagsList.find(_.language == "en").get.tags.size should be(1)

    tagsList.find(_.language == "unknown").get.tags.head should be(
      "språknøytral")
    tagsList.find(_.language == "nb").get.tags.head should be("bokmålspoesi")
    tagsList.find(_.language == "nn").get.tags.head should be("nynorsk poesi")
    tagsList.find(_.language == "en").get.tags.head should be("english poetry")

  }

  test("That getISO639 returns None for uparseable language string") {
    service.getISO639("klingon") should be(None)
  }

  test("That getISO639 returns None for unknown language") {
    service.getISO639("http://psi.oasis-open.org/iso/639/#klingon") should be(
      None)
  }

  test("That getISO639 returns correct language for known language") {
    service.getISO639("http://psi.oasis-open.org/iso/639/#eng") should be(
      Some("en"))
  }

  def mockRequestWithResponse(response: HttpResponse[String]) = {
    val req = mock[HttpRequest]
    when(req.asString).thenReturn(response)
    when(req.url).thenReturn("http://www.mockrequest.com")
    req
  }
}
