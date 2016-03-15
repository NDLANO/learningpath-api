package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{ValidationMessage, Title}

class TitleValidator {
  val MISSING_TITLE = "At least one title is required."

  val noHtmlTextValidator = new TextValidator(allowHtml = false)
  val languageValidator = new LanguageValidator

  def validate(titles: List[Title]): List[ValidationMessage] = {
    titles.isEmpty match {
      case true => List(ValidationMessage("title", MISSING_TITLE))
      case false => titles.flatMap(title => validate(title))
    }
  }

  private def validate(title: Title): List[ValidationMessage] = {
    noHtmlTextValidator.validate("title.title", title.title).toList :::
      languageValidator.validate("title.language", title.language).toList
  }
}
