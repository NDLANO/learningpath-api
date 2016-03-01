package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.Description
import no.ndla.learningpathapi.model.ValidationMessage

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
    val descriptionResult = if (description.description.isEmpty) Some(ValidationMessage("description.description", DESCRIPTION_EMPTY)) else None
    List(descriptionResult, description.language.flatMap(language => LanguageValidator.validate("description.language", language))).flatMap(nonEmpty => nonEmpty)
  }
}
