package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{Title, ValidationMessage}

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
    validateTitleText(title.title).toList :::
    validateTitleLanguage(title.language).toList
  }

  def validateTitleText(text: String): Option[ValidationMessage] = {
    text.isEmpty match {
      case true => Some(ValidationMessage("title.title", TITLE_EMPTY))
      case false => TextValidator.validateNoHtmlTags("title.title", text)
    }
  }

  def validateTitleLanguage(language: Option[String]): Option[ValidationMessage] = {
    language.flatMap(language => LanguageValidator.validate("title.language", language))
  }
}
