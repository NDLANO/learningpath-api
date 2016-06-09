package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain.ValidationException
import no.ndla.mapping.ISO639Mapping

object LanguageValidator {
  def validate(fieldPath: String, languageCodeOpt: Option[String]): Option[String] = {
    new LanguageValidator().validate(fieldPath, languageCodeOpt) match {
      case Some(validationMessage) => throw new ValidationException(errors = validationMessage :: Nil)
      case None => languageCodeOpt
    }
  }
}

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
