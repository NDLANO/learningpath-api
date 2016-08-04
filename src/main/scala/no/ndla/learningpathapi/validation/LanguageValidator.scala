package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.integration.MappingApiClient
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain.ValidationException

trait LanguageValidator {
  this : MappingApiClient =>
  val languageValidator : LanguageValidator

  class LanguageValidator {
    def validate(fieldPath: String, languageCodeOpt: Option[String]): Option[ValidationMessage] = {
      languageCodeOpt match {
        case None => None
        case Some(languageCode) => {
          languageCode.nonEmpty && mappingApiClient.languageCodeSupported6391(languageCode) match {
            case true => None
            case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
          }
        }
      }
    }
  }

  object LanguageValidator {
    def validate(fieldPath: String, languageCodeOpt: Option[String]): Option[String] = {
      languageValidator.validate(fieldPath, languageCodeOpt) match {
        case Some(validationMessage) => throw new ValidationException(errors = validationMessage :: Nil)
        case None => languageCodeOpt
      }
    }
  }
}