package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.business.UserData
import no.ndla.learningpathapi.model.NdlaUserName
import org.json4s.native.Serialization._

import scalaj.http.Http

class AuthUserDataClient extends UserData {

  implicit val formats = org.json4s.DefaultFormats
  val userNameEndpoint = s"http://${LearningpathApiProperties.AuthHost}/auth/about/:user_id"

  override def getUserName(userId: String): NdlaUserName = {
    val response = Http(userNameEndpoint.replace(":user_id", userId)).asString
    if(response.isError) {
      throw new RuntimeException(s"Could not find user-information for user $userId")
    }

    read[NdlaUserName](response.body)
  }
}
