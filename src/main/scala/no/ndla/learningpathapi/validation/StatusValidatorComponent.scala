package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.ValidationMessage
import no.ndla.learningpathapi.model.LearningPathStatus


trait StatusValidatorComponent {
  val statusValidator: StatusValidator

  class StatusValidator {

    /*
    StatusValidator.validate(status) match {
      case None => this
      case Some(result) => throw new ValidationException(errors = List(result))
    }
     */


    def validate(status: String): Option[ValidationMessage] = {
      LearningPathStatus.valueOf(status).isEmpty match {
        case true => Some(ValidationMessage("status", s"'$status' is not a valid publishingstatus."))
        case false => None
      }
    }
  }
}
