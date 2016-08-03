package no.ndla.learningpathapi.validation


import no.ndla.learningpathapi.model.api.{ValidationMessage}
import no.ndla.learningpathapi.model.domain._

trait LearningStepValidator {
  this : TitleValidator with LanguageValidator =>
  val learningStepValidator : LearningStepValidator
  class LearningStepValidator {
    val noHtmlTextValidator = new TextValidator(allowHtml = false)
    val basicHtmlTextValidator = new TextValidator(allowHtml = true)

    val MISSING_DESCRIPTION_OR_EMBED_URL = "A learningstep is required to have either a description, embedContent or both."

    def validate(newLearningStep: LearningStep): LearningStep = {
      validateMessages(newLearningStep) match {
        case head :: tail => throw new ValidationException(errors = head :: tail)
        case _ => newLearningStep
      }
    }

    def validateMessages(newLearningStep: LearningStep): Seq[ValidationMessage] = {
      titleValidator.validate(newLearningStep.title) ++
        validateDescription(newLearningStep.description) ++
        validateEmbedUrl(newLearningStep.embedUrl) ++
        validateLicense(newLearningStep.license).toList ++
        validateThatDescriptionOrEmbedUrlOrBothIsDefined(newLearningStep).toList
    }


    def validateDescription(descriptions: Seq[Description]): Seq[ValidationMessage] = {
      descriptions.isEmpty match {
        case true => List()
        case false => descriptions.flatMap(description => {
          basicHtmlTextValidator.validate("description.description", description.description).toList :::
            languageValidator.validate("description.language", description.language).toList
        })
      }
    }

    def validateEmbedUrl(embedContents: Seq[EmbedUrl]): Seq[ValidationMessage] = {
      embedContents.flatMap(embedContent => {
        noHtmlTextValidator.validate("embedContent.url", embedContent.url).toList :::
          languageValidator.validate("embedContent.language", embedContent.language).toList
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
        case true => Some(ValidationMessage("description|embedContent", MISSING_DESCRIPTION_OR_EMBED_URL))
        case false => None
      }
    }
  }
}
