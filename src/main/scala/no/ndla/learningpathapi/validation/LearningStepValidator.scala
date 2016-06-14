package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain.{EmbedUrl, StepType}

class LearningStepValidator {
  val noHtmlTextValidator = new TextValidator(allowHtml = false)
  val basicHtmlTextValidator = new TextValidator(allowHtml = true)
  val languageValidator = new LanguageValidator
  val titleValidator = new TitleValidator

  val MISSING_DESCRIPTION_OR_EMBED_URL = "A learningstep is required to have either a description, embedContent or both."

  def validate(updatedLearningStep: UpdatedLearningStep): Seq[ValidationMessage] = {
    titleValidator.validate(updatedLearningStep.title) ++
      validateDescription(updatedLearningStep.description) ++
      validateEmbedContent(updatedLearningStep.embedContent) ++
      validateStepType(updatedLearningStep.`type`).toList ++
      validateLicense(updatedLearningStep.license).toList ++
      validateThatDescriptionOrEmbedUrlOrBothIsDefined(updatedLearningStep).toList
  }

  def validate(newLearningStep: NewLearningStep): Seq[ValidationMessage] = {
    titleValidator.validate(newLearningStep.title) ++
      validateDescription(newLearningStep.description) ++
      validateEmbedContent(newLearningStep.embedContent) ++
      validateStepType(newLearningStep.`type`).toList ++
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

  def validateEmbedContent(embedContents: Seq[EmbedContent]): Seq[ValidationMessage] = {
    embedContents.flatMap(embedContent => {
      noHtmlTextValidator.validate("embedContent.url", embedContent.url).toList :::
        languageValidator.validate("embedContent.language", embedContent.language).toList
    })
  }

  def validateStepType(stepType: String): Option[ValidationMessage] = {
    StepType.valueOf(stepType).isEmpty match {
      case true => Some(ValidationMessage("type", s"'$stepType' is not a valid steptype."))
      case false => None
    }
  }

  def validateLicense(licenseOpt: Option[String]): Option[ValidationMessage] = {
    licenseOpt match {
      case None => None
      case Some(license) => noHtmlTextValidator.validate("license", license)
    }
  }

  def validateThatDescriptionOrEmbedUrlOrBothIsDefined(newLearningStep: NewLearningStep): Option[ValidationMessage] = {
    newLearningStep.description.isEmpty && newLearningStep.embedContent.isEmpty match {
      case true => Some(ValidationMessage("description|embedContent", MISSING_DESCRIPTION_OR_EMBED_URL))
      case false => None
    }
  }

  def validateThatDescriptionOrEmbedUrlOrBothIsDefined(updatedLearningStep: UpdatedLearningStep): Option[ValidationMessage] = {
    updatedLearningStep.description.isEmpty && updatedLearningStep.embedContent.isEmpty match {
      case true => Some(ValidationMessage("description|embedContent", MISSING_DESCRIPTION_OR_EMBED_URL))
      case false => None
    }
  }
}
