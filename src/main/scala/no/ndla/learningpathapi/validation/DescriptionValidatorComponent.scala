package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{ValidationMessage, Description}

trait DescriptionValidatorComponent {
  this: TextValidatorComponent with LanguageValidatorComponent =>
  val basicHtmlDescriptionValidator: DescriptionValidator
  val noHtmlDescriptionValidator: DescriptionValidator

  class DescriptionValidator(allowHtml: Boolean) {
    val MISSING_DESCRIPTION = "At least one description is required."

    val textValidator = allowHtml match {
      case true => basicHtmlTextValidator
      case false => noHtmlTextValidator
    }

    def validateOptional(descriptions: List[Description]): List[ValidationMessage] = {
      descriptions.isEmpty match {
        case true => List()
        case false => descriptions.flatMap(description => validate(description))
      }
    }

    def validateRequired(descriptions: List[Description]): List[ValidationMessage] = {
      descriptions.isEmpty match {
        case true => List(ValidationMessage("description", MISSING_DESCRIPTION))
        case false => descriptions.flatMap(description => validate(description))
      }
    }

    def validate(description: Description): List[ValidationMessage] = {
      textValidator.validate("description.description", description.description).toList :::
      languageValidator.validate("description.language", description.language).toList
    }
  }
}
