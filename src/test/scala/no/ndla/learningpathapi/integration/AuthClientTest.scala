package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite, UnitTestEnvironment}
import org.mockito.Mockito._

import scalaj.http.{HttpRequest, HttpResponse}
class AuthClientTest extends UnitSuite with UnitTestEnvironment{

  val parseableResponse =
    """
      |{
      |  "first_name": "Fornavn",
      |  "middle_name": "Mellomnavn",
      |  "last_name": "Etternavn"
      |}
    """.stripMargin

  var client: AuthClient = _

  override def beforeEach() = {
    client = new AuthClient
  }

  test("That getUserNameFromRequest returns Unknown-user when http error") {
    val request = mock[HttpRequest]
    val response = mock[HttpResponse[String]]

    when(request.asString).thenReturn(response)
    when(request.url).thenReturn("ABC")
    when(response.isError).thenReturn(true)
    when(response.code).thenReturn(111)
    when(response.statusLine).thenReturn("FEIL")

    val ndlaUserName = client.getUserNameFromRequest(request)
    ndlaUserName.first_name should equal(Some("Unknown"))
    ndlaUserName.middle_name should be(None)
    ndlaUserName.last_name should be(None)
  }

  test("That getUserNameFromRequest returns Unknown-user when parse-error") {
    val request = mock[HttpRequest]
    val response = mock[HttpResponse[String]]

    when(request.asString).thenReturn(response)
    when(request.url).thenReturn("ABC")
    when(response.isError).thenReturn(false)
    when(response.body).thenReturn("Unparseable string")


    val ndlaUserName = client.getUserNameFromRequest(request)
    ndlaUserName.first_name should equal(Some("Unknown"))
    ndlaUserName.middle_name should be(None)
    ndlaUserName.last_name should be(None)
  }


  test("That getUserNameFromRequest returns a NdlaUserName when OK") {
    val request = mock[HttpRequest]
    val response = mock[HttpResponse[String]]

    when(request.asString).thenReturn(response)
    when(response.isError).thenReturn(false)
    when(response.body).thenReturn(parseableResponse)

    val ndlaUserName = client.getUserNameFromRequest(request)
    ndlaUserName.first_name.get should equal("Fornavn")
    ndlaUserName.middle_name.get should equal("Mellomnavn")
    ndlaUserName.last_name.get should equal("Etternavn")
  }
}
