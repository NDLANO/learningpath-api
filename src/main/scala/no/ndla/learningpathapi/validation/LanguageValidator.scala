package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.ValidationMessage
import no.ndla.mapping.ISO639Mapping

class LanguageValidator {
  def validate(fieldPath: String, languageCodeOpt: Option[String]): Option[ValidationMessage] = {
    languageCodeOpt match {
      case None => None
      case Some(languageCode) => {
        languageCode.nonEmpty && ISO639Mapping.languageCodeSupported(languageCode) match {
          case true => None
          case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
        }
      }
    }
  }
}
