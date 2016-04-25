package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite, UnitTestEnvironment}
import org.mockito.Mockito._
import org.scalatest.TryValues._

import scalaj.http.{HttpRequest, HttpResponse}

class OEmbedClientTest extends UnitSuite with UnitTestEnvironment {

  val DefaultOembed = OEmbed (
    `type` = "rich",
    version = "1.0",
    title = Some("Title"),
    description = Some("Description"),
    authorName = Some("AuthorName"),
    authorUrl = None,
    providerName = Some("ProviderName"),
    providerUrl = None,
    cacheAge = None,
    thumbnailUrl = None,
    thumbnailWidth = None,
    thumbnailHeight = None,
    url = Some("http://some.url.com"),
    width = Some(350),
    height = Some(600),
    html = Some("<iframe>"))

    val parseableResponse =
      """
        |{
        | "someKey": "someValue"
        |}
      """.stripMargin

  val htmlEmbed = """<iframe src='http://ndla.no/en/node/137355/oembed' allowfullscreen></iframe>"""
  val correctResponse =
    s"""
      |{
      |    "type": "rich",
      |    "version": "1.0",
      |    "title": "Yahya Hassan – dansk poet og rebell",
      |    "providerName": "NDLA - Nasjonal digital læringsarena",
      |    "providerUrl": "http://ndla.no/",
      |    "width": 700,
      |    "height": 800,
      |    "html": "$htmlEmbed"
      |}
    """.stripMargin

  var client: OEmbedClient = _
  override def beforeEach() = {
    client = new OEmbedClient
  }

  test("That getHtml returns Failure when None html") {
    val result = client.getHtml(DefaultOembed.copy(html = None))
    result should be a 'failure
    result.failure.exception.getMessage should equal("No embed-code in oembedResponse")
  }

  test("That getHtml returns the html-snippet when not None") {
    val result = client.getHtml(DefaultOembed.copy(html = Some("<sometag>blah</sometag>")))
    result should be a 'success
    result.get should equal ("<sometag>blah</sometag>")
  }

  test("That parseResponse returns exception when response is not parseable") {
    val responseMock = mock[HttpResponse[String]]
    when(responseMock.body).thenReturn("asdf")

    val result = client.parseResponse(responseMock)
    result should be a 'failure
    result.failure.exception.getMessage should equal ("Unreadable response asdf")
  }

  test("That parseResponse returns exception when response is parseable, but on incorrect format") {
    val responseMock = mock[HttpResponse[String]]
    when(responseMock.body).thenReturn(parseableResponse)

    val result = client.parseResponse(responseMock)
    result should be a 'failure
    result.failure.exception.getMessage should equal (s"Unreadable response $parseableResponse")
  }

  test("That parseResponse returns an OEmbed-object when response is as expected") {
    val responseMock = mock[HttpResponse[String]]
    when(responseMock.body).thenReturn(correctResponse)

    val result = client.parseResponse(responseMock)
    result should be a 'success
    result.get.`type` should equal ("rich")
    result.get.width should be (Some(700))
  }

  test("That doRequest returns a failure when http-error") {
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpRequestMock.url).thenReturn("someUrl")

    when(httpResponseMock.isError).thenReturn(true)
    when(httpResponseMock.code).thenReturn(123)
    when(httpResponseMock.statusLine).thenReturn("status")

    val result = client.doRequest(httpRequestMock)
    result should be a 'failure
    result.failure.exception.getMessage should equal ("Got 123 status when calling someUrl")
  }

  test("That doRequest returns a response when no error") {
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)

    val result = client.doRequest(httpRequestMock)
    result should be a 'success
    result.get should be (httpResponseMock)
  }

  test("That getHtmlEmbedCodeForRequest returns html-snippet") {
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpResponseMock.isError).thenReturn(false)
    when(httpResponseMock.body).thenReturn(correctResponse)

    val result = client.getHtmlEmbedCodeForRequest(httpRequestMock)
    result should be a 'success
    result.get should equal (htmlEmbed)
  }
}
