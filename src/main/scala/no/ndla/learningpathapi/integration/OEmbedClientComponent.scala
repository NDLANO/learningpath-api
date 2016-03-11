package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.domain.HttpRequestException
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Success, Try}
import scalaj.http.{HttpResponse, Http, HttpRequest}

trait OEmbedClientComponent {
  val oEmbedClient: OEmbedClient

  class OEmbedClient extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats
    val oEmbedEndpoint = s"http://${LearningpathApiProperties.OEmbedHost}/oembed?url=:embed_url"

    def getHtmlEmbedCodeForUrl(url: String): String = {
      getHtmlEmbedCodeForRequest(Http(oEmbedEndpoint.replace(":embed_url", url))) match {
        case Success(embedCode) => embedCode
        case Failure(exception) => throw exception
      }
    }

    def getHtmlEmbedCodeForRequest(request: HttpRequest): Try[String] = {
      for {
        response <- doRequest(request)
        oembed <- parseResponse(response)
        url <- getHtml(oembed)
      } yield url
    }

    def doRequest(request: HttpRequest): Try[HttpResponse[String]] = {
      logger.debug(s"Doing request to ${request.url}")
      val response = request.asString
      response.isError match {
        case true => Failure(new HttpRequestException(s"Got ${response.code} ${response.statusLine} when calling ${request.url}"))
        case false => Success(response)
      }
    }

    def parseResponse(response: HttpResponse[String]): Try[OEmbed] = {
      parseOpt(response.body).flatMap(_.camelizeKeys.extractOpt[OEmbed]) match {
        case Some(embed) => Success(embed)
        case None => Failure(new HttpRequestException(s"Unreadable response ${response.body}"))
      }
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

