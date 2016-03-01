package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.Title
import no.ndla.learningpathapi.model.ValidationMessage

object TitleValidator {
  val TITLE_EMPTY = "Required value title is empty."
  val MISSING_TITLE = "At least one title is required."

  def validate(titles: List[Title]): List[ValidationMessage] = {
    titles.isEmpty match {
      case true => List(ValidationMessage("title", MISSING_TITLE))
      case false => titles.flatMap(title => validate(title))
    }
  }

  def validate(title: Title): List[ValidationMessage] = {
    val titleResult = if (title.title.isEmpty) Some(ValidationMessage("title.title", TITLE_EMPTY)) else None
    List(titleResult, title.language.flatMap(title => LanguageValidator.validate("title.language", title))).flatMap(nonEmpty => nonEmpty)
  }
}
