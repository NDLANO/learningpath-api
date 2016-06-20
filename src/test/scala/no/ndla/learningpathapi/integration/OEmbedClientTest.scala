package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import org.mockito.Mockito._
import org.scalatest.TryValues._

import scala.util.Success
import scalaj.http.HttpRequest

class OEmbedClientTest extends UnitSuite with UnitTestEnvironment {

  val DefaultOembed = OEmbed(
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

  val htmlEmbed = """<iframe src='http://ndla.no/en/node/137355/oembed' allowfullscreen></iframe>"""

  override val oEmbedClient: OEmbedClient = new OEmbedClient

  test("That getHtml returns Failure when None html") {
    val result = oEmbedClient.getHtml(DefaultOembed.copy(html = None))
    result should be a 'failure
    result.failure.exception.getMessage should equal("No embed-code in oembedResponse")
  }

  test("That getHtml returns the html-snippet when not None") {
    val result = oEmbedClient.getHtml(DefaultOembed.copy(html = Some("<sometag>blah</sometag>")))
    result should be a 'success
    result.get should equal("<sometag>blah</sometag>")
  }

  test("That getHtmlEmbedCodeForRequest returns html-snippet") {
    val httpRequestMock = mock[HttpRequest]
    when(ndlaClient.fetch[OEmbed](httpRequestMock)).thenReturn(Success(DefaultOembed.copy(html = Some(htmlEmbed))))

    val result = oEmbedClient.getHtmlEmbedCodeForRequest(httpRequestMock)
    result should be a 'success
    result.get should equal(htmlEmbed)
  }
}
