package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.StepType
import no.ndla.learningpathapi.{Description, EmbedUrl, NewLearningStep, ValidationMessage}

class NewLearningStepValidator {
  val noHtmlTextValidator = new TextValidator(allowHtml = false)
  val basicHtmlTextValidator = new TextValidator(allowHtml = true)
  val languageValidator = new LanguageValidator
  val titleValidator = new TitleValidator

  val MISSING_DESCRIPTION_OR_EMBED_URL = "A learningstep is required to have either a description, embedUrl or both."

  def validate(newLearningStep: NewLearningStep): List[ValidationMessage] = {
    titleValidator.validate(newLearningStep.title) :::
      validateDescription(newLearningStep.description) :::
      validateEmbedUrl(newLearningStep.embedUrl) :::
      validateStepType(newLearningStep.`type`).toList :::
      validateLicense(newLearningStep.license).toList :::
      validateThatDescriptionOrEmbedUrlOrBothIsDefined(newLearningStep).toList
  }

  def validateDescription(descriptions: List[Description]): List[ValidationMessage] = {
    descriptions.isEmpty match {
      case true => List()
      case false => descriptions.flatMap(description => {
        basicHtmlTextValidator.validate("description.description", description.description).toList :::
          languageValidator.validate("description.language", description.language).toList
      })
    }
  }

  def validateEmbedUrl(embedUrls: List[EmbedUrl]): List[ValidationMessage] = {
    embedUrls.flatMap(embedUrl => {
      noHtmlTextValidator.validate("embedUrl.url", embedUrl.url).toList :::
        languageValidator.validate("embedUrl.language", embedUrl.language).toList
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
    newLearningStep.description.isEmpty && newLearningStep.embedUrl.isEmpty match {
      case true => Some(ValidationMessage("description|embedUrl", MISSING_DESCRIPTION_OR_EMBED_URL))
      case false => None
    }
  }
}
