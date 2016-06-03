package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.{Title, ValidationMessage}

class TitleValidator(titleRequired: Boolean = true) {
  val MISSING_TITLE = "At least one title is required."

  val noHtmlTextValidator = new TextValidator(allowHtml = false)
  val languageValidator = new LanguageValidator

  def validate(titles: List[Title]): List[ValidationMessage] = {
    (titleRequired, titles.isEmpty) match {
      case (false, true) => List()
      case (true, true) => List(ValidationMessage("title", MISSING_TITLE))
      case (_, false) => titles.flatMap(title => validate(title))
    }
  }

  private def validate(title: Title): List[ValidationMessage] = {
    noHtmlTextValidator.validate("title.title", title.title).toList :::
      languageValidator.validate("title.language", title.language).toList
  }
}
