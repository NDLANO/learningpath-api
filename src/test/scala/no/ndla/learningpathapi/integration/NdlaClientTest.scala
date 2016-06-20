package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}

import scalaj.http.{HttpRequest, HttpResponse}
import org.mockito.Mockito._
import org.scalatest.TryValues._

class NdlaClientTest extends UnitSuite with UnitTestEnvironment {

  case class TestObject(id: String, verdi: String)
  val ParseableContent =
    """
      |{
      |  "id": "1",
      |  "verdi": "This is the value"
      |}
    """.stripMargin

  override val ndlaClient: NdlaClient = new NdlaClient

  test("That a HttpRequestException is returned when receiving an http-error") {
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpRequestMock.url).thenReturn("someUrl")

    when(httpResponseMock.isError).thenReturn(true)
    when(httpResponseMock.code).thenReturn(123)
    when(httpResponseMock.statusLine).thenReturn("status")

    val result = ndlaClient.fetch[TestObject](httpRequestMock)

    result should be a 'failure
    result.failure.exception.getMessage should equal("Got 123 status when calling someUrl")
  }

  test("That a HttpRequestException is returned when response is not parseable") {
    val unparseableResponse = "This string cannot be parsed to a TestObject"
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpResponseMock.isError).thenReturn(false)
    when(httpResponseMock.body).thenReturn(unparseableResponse)

    val result = ndlaClient.fetch[TestObject](httpRequestMock)
    result should be a 'failure
    result.failure.exception.getMessage should equal (s"Unreadable response $unparseableResponse")
  }

  test("That a testObject is returned when no error is returned and content is parseable") {
    val httpRequestMock = mock[HttpRequest]
    val httpResponseMock = mock[HttpResponse[String]]

    when(httpRequestMock.asString).thenReturn(httpResponseMock)
    when(httpResponseMock.isError).thenReturn(false)
    when(httpResponseMock.body).thenReturn(ParseableContent)

    val result = ndlaClient.fetch[TestObject](httpRequestMock)
    result should be a 'success
    result.get.id should equal ("1")
    result.get.verdi should equal("This is the value")
  }
}

