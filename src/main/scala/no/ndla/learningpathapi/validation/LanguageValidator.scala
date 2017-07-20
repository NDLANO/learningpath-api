/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain.ValidationException
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.learningpathapi.model.domain.Language

trait LanguageValidator {
  val languageValidator : LanguageValidator

  class LanguageValidator {
    private def languageCodeSupported6391(languageCode: String): Boolean =
      get6391CodeFor6392CodeMappings.exists(_._2 == languageCode)

    def validate(fieldPath: String, languageCodeOpt: Option[String]): Option[ValidationMessage] = {
      languageCodeOpt match {
        case None => None
        case Some(languageCode) => {
          languageCode.nonEmpty && languageCodeSupported6391(languageCode) match {
            case true => None
            case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
          }
        }
      }
    }
  }

  object LanguageValidator {
    def validate(fieldPath: String, languageCodeOpt: Option[String]): Option[String] = {
      languageValidator.validate(fieldPath, languageCodeOpt) match {
        case Some(validationMessage) => throw new ValidationException(errors = validationMessage :: Nil)
        case None => languageCodeOpt
      }
    }

    def checkIfLanguageIsSupported(supportedLanguages: Seq[String], language: String) = {
      if (!supportedLanguages.contains(language) && language != Language.AllLanguages) {
        val validationMessage = ValidationMessage("language", s"Language '$language' is not a supported value.")
        throw new ValidationException(errors = validationMessage :: Nil)
      }
    }
  }
}