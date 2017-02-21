/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage

class UrlValidator() {
  val noHtmlTextValidator = new TextValidator(allowHtml = false)

  def validate(fieldPath: String, text: String): Seq[ValidationMessage] = {
    nonEmptyText(fieldPath, text) ++
      noHtmlInText(fieldPath, text) ++
      startsWithHttps(fieldPath, text)
  }

  private def nonEmptyText(fieldPath: String, text: String): Seq[ValidationMessage] = {
    text.isEmpty match {
      case true => List(ValidationMessage(fieldPath, "Required field is empty."))
      case false => List()
    }
  }

  private def noHtmlInText(fieldPath: String, text: String): Seq[ValidationMessage] = {
    noHtmlTextValidator.validate(fieldPath, text) match {
      case Some(x) => List(x)
      case _ => List()
    }
  }

  private def startsWithHttps(fieldPath: String, text: String): Seq[ValidationMessage] = {
    text.startsWith("https") match {
      case true => List()
      case false => List(ValidationMessage(fieldPath, "Illegal Url. All Urls must start with https."))
    }
  }
}
