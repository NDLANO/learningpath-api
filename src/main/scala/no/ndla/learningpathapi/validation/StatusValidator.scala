package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.ValidationMessage
import no.ndla.learningpathapi.model.LearningPathStatus

object StatusValidator {
  def validate(status: String): Option[ValidationMessage] = {
    LearningPathStatus.valueOf(status).isEmpty match {
      case true => Some(ValidationMessage("status", s"'$status' is not a valid publishingstatus."))
      case false => None
    }
  }
}
