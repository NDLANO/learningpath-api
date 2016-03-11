package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.domain.NdlaUserName
import org.json4s.native.Serialization._

import scala.util.{Failure, Success, Try}
import scalaj.http.{HttpRequest, Http}

trait AuthClientComponent {
  val authClient: AuthClient

  val unknownUser = NdlaUserName(Some("Unknown"), None, None)

  class AuthClient extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats
    val userNameEndpoint = s"http://${LearningpathApiProperties.AuthHost}/auth/about/:user_id"

    def getUserName(userId: String): NdlaUserName = {
      getUserNameFromRequest(Http(userNameEndpoint.replace(":user_id", userId)))
    }

    def getUserNameFromRequest(request: HttpRequest): NdlaUserName = {
      val response = request.asString
      response.isError match {
        case true => {
          logger.warn(s"Could not find user-information from url ${request.url}. Received http status ${response.code} ${response.statusLine}")
          unknownUser
        }
        case false => {
          val body = response.body
          Try(read[NdlaUserName](body)) match {
            case Success(username) => username
            case Failure(ex) => {
              logger.warn(s"Could not parse response $body from url ${request.url}")
              unknownUser
            }
          }
        }
      }
    }
  }
}
