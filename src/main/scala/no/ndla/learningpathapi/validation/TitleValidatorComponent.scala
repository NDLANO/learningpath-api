package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{ValidationMessage, Title}


trait TitleValidatorComponent {
  this: TextValidatorComponent with LanguageValidatorComponent =>
  val titleValidator: TitleValidator

  class TitleValidator {
    val MISSING_TITLE = "At least one title is required."

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
}
