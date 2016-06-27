package no.ndla.learningpathapi.batch.integration

import no.ndla.learningpathapi.UnitSuite
import no.ndla.learningpathapi.batch.BatchTestEnvironment
import org.mockito.Mockito._

import scalaj.http.{HttpRequest, HttpResponse}

class KeywordsServiceTest extends UnitSuite with BatchTestEnvironment {
  val parseableResponse = """
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
  }

  test("That forRequest returns empty list when http-error") {
    val resp = mock[HttpResponse[String]]
    when(resp.isError).thenReturn(true)

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
    tagsList.find(_.language.isEmpty).get.tag.size should be (1)
    tagsList.find(_.language.contains("nb")).get.tag.size should be (1)
    tagsList.find(_.language.contains("nn")).get.tag.size should be (1)
    tagsList.find(_.language.contains("en")).get.tag.size should be (1)

    tagsList.find(_.language.isEmpty).get.tag.head should be ("språknøytral")
    tagsList.find(_.language.contains("nb")).get.tag.head should be ("bokmålspoesi")
    tagsList.find(_.language.contains("nn")).get.tag.head should be ("nynorsk poesi")
    tagsList.find(_.language.contains("en")).get.tag.head should be ("english poetry")

  }

  test("That getISO639 returns None for uparseable language string") {
    service.getISO639("klingon") should be(None)
  }

  test("That getISO639 returns None for unknown language") {
    service.getISO639("http://psi.oasis-open.org/iso/639/#klingon") should be(None)
  }

  test("That getISO639 returns correct language for known language") {
    service.getISO639("http://psi.oasis-open.org/iso/639/#eng") should be(Some("en"))
  }

  test("Live") {
    service.forNodeId(122564)
  }

  def mockRequestWithResponse(response: HttpResponse[String]) = {
    val req = mock[HttpRequest]
    when(req.asString).thenReturn(response)
    when(req.url).thenReturn("http://www.mockrequest.com")
    req
  }
}
