package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{Description, ValidationMessage}

object DescriptionValidator {
  val DESCRIPTION_EMPTY = "Required value description is empty."
  val MISSING_DESCRIPTION = "At least one description is required."

  def validate(descriptions: List[Description]): List[ValidationMessage] = {
    descriptions.isEmpty match {
      case true => List(ValidationMessage("description", MISSING_DESCRIPTION))
      case false => descriptions.flatMap(description => validate(description))
    }
  }

  def validate(description: Description): List[ValidationMessage] = {
    validateDescriptionText(description.description).toList :::
    validateDescriptionLanguage(description.language).toList
  }

  def validateDescriptionText(text: String): Option[ValidationMessage] = {
    text.isEmpty match {
      case true => Some(ValidationMessage("description.description", DESCRIPTION_EMPTY))
      case false => TextValidator.validateOnlyBasicHtmlTags("description.description", text)
    }
  }

  def validateDescriptionLanguage(language: Option[String]): Option[ValidationMessage] = {
    language.flatMap(language => LanguageValidator.validate("description.language", language))
  }
}
