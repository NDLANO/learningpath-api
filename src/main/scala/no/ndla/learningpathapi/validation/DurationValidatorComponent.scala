package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.ValidationMessage

trait DurationValidatorComponent {
  val durationValidator: DurationValidator

  class DurationValidator {
    val DURATION_INVALID = "Required value duration must be greater than 0."

    def validate(duration: Int): Option[ValidationMessage] = {
      if(duration < 1) {
        Some(ValidationMessage("duration", DURATION_INVALID))
      } else {
        None
      }
    }
  }
}
