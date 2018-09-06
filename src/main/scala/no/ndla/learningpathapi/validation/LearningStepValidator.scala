/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain._

trait LearningStepValidator {
  this: TitleValidator with LanguageValidator =>
  val learningStepValidator: LearningStepValidator

  class LearningStepValidator {
    val noHtmlTextValidator = new TextValidator(allowHtml = false)
    val basicHtmlTextValidator = new TextValidator(allowHtml = true)
    val urlValidator = new UrlValidator()

    val MISSING_DESCRIPTION_OR_EMBED_URL =
      "A learningstep is required to have either a description, embedUrl or both."

    def validate(newLearningStep: LearningStep, allowUnknownLanguage: Boolean = false): LearningStep = {
      validateLearningStep(newLearningStep, allowUnknownLanguage) match {
        case head :: tail =>
          throw new ValidationException(errors = head :: tail)
        case _ => newLearningStep
      }
    }

    def validateLearningStep(newLearningStep: LearningStep, allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      titleValidator.validate(newLearningStep.title, allowUnknownLanguage) ++
        validateDescription(newLearningStep.description, allowUnknownLanguage) ++
        validateEmbedUrl(newLearningStep.embedUrl, allowUnknownLanguage) ++
        validateLicense(newLearningStep.license).toList ++
        validateThatDescriptionOrEmbedUrlOrBothIsDefined(newLearningStep).toList
    }

    def validateDescription(descriptions: Seq[Description], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      descriptions.isEmpty match {
        case true => List()
        case false =>
          descriptions.flatMap(description => {
            basicHtmlTextValidator
              .validate("description", description.description)
              .toList :::
              languageValidator
              .validate("language", description.language, allowUnknownLanguage)
              .toList
          })
      }
    }

    def validateEmbedUrl(embedUrls: Seq[EmbedUrl], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      embedUrls.flatMap(embedUrl => {
        urlValidator.validate("embedUrl.url", embedUrl.url).toList :::
          languageValidator
          .validate("language", embedUrl.language, allowUnknownLanguage)
          .toList
      })
    }

    def validateLicense(licenseOpt: Option[String]): Option[ValidationMessage] = {
      licenseOpt match {
        case None => None
        case Some(license) => {
          noHtmlTextValidator.validate("license", license)
        }
      }
    }

    def validateThatDescriptionOrEmbedUrlOrBothIsDefined(newLearningStep: LearningStep): Option[ValidationMessage] = {
      newLearningStep.description.isEmpty && newLearningStep.embedUrl.isEmpty match {
        case true =>
          Some(ValidationMessage("description|embedUrl", MISSING_DESCRIPTION_OR_EMBED_URL))
        case false => None
      }
    }
  }
}
