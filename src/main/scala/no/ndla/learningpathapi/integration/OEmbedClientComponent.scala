package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.domain.HttpRequestException
import org.json4s.jackson.JsonMethods._

import scala.util.{Failure, Success, Try}
import scalaj.http.{HttpResponse, Http, HttpRequest}

trait OEmbedClientComponent {
  this: NdlaClient =>
  val oEmbedClient: OEmbedClient

  class OEmbedClient extends LazyLogging {
    val oEmbedEndpoint = s"http://${LearningpathApiProperties.OEmbedHost}/oembed?url=:embed_url"

    def getHtmlEmbedCodeForUrl(url: String): Option[String] = {
      getHtmlEmbedCodeForRequest(Http(oEmbedEndpoint.replace(":embed_url", url))) match {
        case Success(embedCode) => Some(embedCode)
        case Failure(exception) => throw exception
      }
    }

    def getHtmlEmbedCodeForRequest(request: HttpRequest): Try[String] = {
      for {
        oembed <- ndlaClient.fetch[OEmbed](request)
        url <- getHtml(oembed)
      } yield url
    }

    def getHtml(oembed: OEmbed): Try[String] = {
      oembed.html match {
        case Some(snippet) => Success(snippet)
        case None => Failure(new HttpRequestException(s"No embed-code in oembedResponse"))
      }
    }
  }
}

case class OEmbed (`type`: String, version: String, title: Option[String], description: Option[String], authorName: Option[String], authorUrl: Option[String], providerName: Option[String], providerUrl: Option[String],
                   cacheAge: Option[Long], thumbnailUrl: Option[String], thumbnailWidth: Option[Long], thumbnailHeight: Option[Long], url: Option[String], width: Option[Long], height: Option[Long], html: Option[String])

