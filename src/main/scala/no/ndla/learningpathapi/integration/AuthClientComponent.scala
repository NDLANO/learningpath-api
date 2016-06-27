package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.domain.NdlaUserName
import org.json4s.jackson.Serialization._

import scala.util.{Failure, Success, Try}
import scalaj.http.{HttpRequest, Http}

trait AuthClientComponent {
  this: NdlaClient =>
  val authClient: AuthClient

  class AuthClient extends LazyLogging {
    val unknownUser = NdlaUserName(Some("Unknown"), None, None)
    val userNameEndpoint = s"http://${LearningpathApiProperties.AuthHost}/auth/about/:user_id"

    def getUserName(userId: String): NdlaUserName = {
      ndlaClient.fetch[NdlaUserName](Http(userNameEndpoint.replace(":user_id", userId))) match {
        case Success(username) => username
        case Failure(ex) => unknownUser
      }
    }
  }
}
