package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.ValidationMessage
import no.ndla.mapping.ISO639Mapping

object LanguageValidator {
  def validate(fieldPath:String, languageCode: String): Option[ValidationMessage] = {
    languageCode.nonEmpty && ISO639Mapping.languageCodeSupported(languageCode) match {
      case true => None
      case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
    }
  }
}
