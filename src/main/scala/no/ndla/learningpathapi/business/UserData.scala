package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.model.NdlaUserName

trait UserData {
  def getUserName(userId: String): NdlaUserName
}
