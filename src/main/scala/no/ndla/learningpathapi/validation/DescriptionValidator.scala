package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{Description, ValidationMessage}

object DescriptionValidator {
  val DESCRIPTION_EMPTY = "Required value description is empty."
  val MISSING_DESCRIPTION = "At least one description is required."

  def validateNoHtml(descriptions: List[Description]): List[ValidationMessage] = {
    validate(descriptions, TextValidator.validateNoHtmlTags)
  }

  def validateBasicHtml(descriptions: List[Description]): List[ValidationMessage] = {
    validate(descriptions, TextValidator.validateOnlyBasicHtmlTags)
  }

  def validate(descriptions: List[Description], validator: (String,String) => Option[ValidationMessage]): List[ValidationMessage] = {
    descriptions.isEmpty match {
      case true => List(ValidationMessage("description", MISSING_DESCRIPTION))
      case false => descriptions.flatMap(description => {
        validateDescriptionText(description.description, validator).toList :::
        validateDescriptionLanguage(description.language).toList
      })
    }
  }

  def validateDescriptionText(text: String, validator: (String, String) => Option[ValidationMessage]): Option[ValidationMessage] = {
    text.isEmpty match {
      case true => Some(ValidationMessage("description.description", DESCRIPTION_EMPTY))
      case false => validator("description.description", text)
    }
  }

  def validateDescriptionLanguage(language: Option[String]): Option[ValidationMessage] = {
    language.flatMap(language => LanguageValidator.validate("description.language", language))
  }
}

