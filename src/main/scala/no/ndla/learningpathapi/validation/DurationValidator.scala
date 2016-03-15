package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage

class DurationValidator {
  val DURATION_INVALID = "Required value duration must be greater than 0."
  val DURATION_REQUIRED = "Required value is empty."

  def validateRequired(durationOpt: Option[Int]): Option[ValidationMessage] = {
    durationOpt match {
      case None => Some(ValidationMessage("duration", DURATION_REQUIRED))
      case Some(duration) => {
        duration < 1 match {
          case true => Some(ValidationMessage("duration", DURATION_INVALID))
          case false => None
        }
      }
    }
  }
}

