package no.ndla.learningpathapi.integration


import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model.domain.HttpRequestException
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Success, Try}
import scalaj.http.{HttpRequest, HttpResponse}

trait NdlaClient {
  val ndlaClient: NdlaClient

  class NdlaClient extends LazyLogging {

    def fetch[A](request: HttpRequest)(implicit mf: Manifest[A]): Try[A] = {
      for {
        httpResponse <- doRequest(request)
        bodyObject <- parseResponse[A](httpResponse)(mf)
      } yield bodyObject
    }

    private def doRequest(request: HttpRequest): Try[HttpResponse[String]] = {
      val response = request.asString
      response.isError match {
        case false => Success(response)
        case true => {
          logger.warn(s"Received error on url = ${request.url}. Received http status ${response.code} ${response.statusLine}")
          Failure(new HttpRequestException(s"Got ${response.code} ${response.statusLine} when calling ${request.url}", Some(response)))
        }
      }
    }

    private def parseResponse[A](response: HttpResponse[String])(implicit mf: Manifest[A]): Try[A] = {
      implicit val formats = org.json4s.DefaultFormats

      parseOpt(response.body).flatMap(_.camelizeKeys.extractOpt[A]) match {
        case Some(extracted) => Success(extracted)
        case None => {
          logger.warn(s"Could not parse response ${response.body}")
          Failure(new HttpRequestException(s"Unreadable response ${response.body}", Some(response)))
        }
      }
    }
  }

}
