/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain.Title

trait TitleValidator {
  this : LanguageValidator =>
  val titleValidator : TitleValidator

  class TitleValidator(titleRequired: Boolean = true) {
    val MISSING_TITLE = "At least one title is required."

    val noHtmlTextValidator = new TextValidator(allowHtml = false)

    def validate(titles: Seq[Title], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      (titleRequired, titles.isEmpty) match {
        case (false, true) => List()
        case (true, true) => List(ValidationMessage("title", MISSING_TITLE))
        case (_, false) => titles.flatMap(title => validate(title, allowUnknownLanguage))
      }
    }

    private def validate(title: Title, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      noHtmlTextValidator.validate("title.title", title.title).toList :::
        languageValidator.validate("title.language", title.language, allowUnknownLanguage).toList
    }
  }
}