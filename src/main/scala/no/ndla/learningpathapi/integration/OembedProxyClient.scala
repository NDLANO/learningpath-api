/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.{ApiGatewayHost, SearchApiHost}
import no.ndla.learningpathapi.model.domain._
import no.ndla.network.NdlaClient
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.write
import org.jsoup.Jsoup
import scalaj.http.{Http, HttpRequest, HttpResponse}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait OembedProxyClient {
  this: NdlaClient =>
  val oembedProxyClient: OembedProxyClient

  class OembedProxyClient extends LazyLogging {
    private val OembedProxyTimeout = 90 * 1000 // 90 seconds
    private val OembedProxyBaseUrl = s"http://$ApiGatewayHost/oembed-proxy/v1"
    implicit val formats: Formats = org.json4s.DefaultFormats

    case class OembedResponse(
        `type`: String,
        version: String,
        title: String,
        width: Int,
        height: Int,
        html: String
    )

    def getIframeUrl(url: String): Try[String] = {
      getOembed(url) match {
        case Failure(ex) => Failure(ex)
        case Success(oembed) =>
          val soup = Jsoup.parse(oembed.html)
          val elem = Option(soup.selectFirst("iframe"))
          Option(elem.map(_.attr("src")).filterNot(_.isEmpty)).flatten match {
            case Some(url) => Success(url)
            case None      => Failure(InvalidOembedResponse(s"Could not parse url in html from oembed-response for '$url'"))
          }
      }
    }

    private def getOembed(url: String): Try[OembedResponse] = {
      get[OembedResponse](s"$OembedProxyBaseUrl/oembed", ("url" -> url))
    }

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(url).timeout(OembedProxyTimeout, OembedProxyTimeout).params(params))
    }
  }

}
