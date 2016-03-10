package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{NewLearningStep, ValidationMessage}

trait NewLearningStepValidatorComponent {
  this: TitleValidatorComponent with DescriptionValidatorComponent with EmbedUrlValidatorComponent with StepTypeValidatorComponent with LicenseValidatorComponent =>
  val newLearningStepValidator: NewLearningStepValidator

  class NewLearningStepValidator {

    val MISSING_DESCRIPTION_OR_EMBED_URL = "A learningstep is required to have either a description, embedUrl or both."

    def validate(newLearningStep: NewLearningStep): List[ValidationMessage] = {
      titleValidator.validate(newLearningStep.title) :::
        basicHtmlDescriptionValidator.validateOptional(newLearningStep.description) :::
        embedUrlValidator.validate(newLearningStep.embedUrl) :::
        stepTypeValidator.validate(newLearningStep.`type`).toList :::
        licenseValidator.validate(newLearningStep.license).toList :::
        checkThatDescriptionOrEmbedUrlOrBothIsDefined(newLearningStep).toList
    }

    def checkThatDescriptionOrEmbedUrlOrBothIsDefined(newLearningStep: NewLearningStep): Option[ValidationMessage] = {
      newLearningStep.description.isEmpty && newLearningStep.embedUrl.isEmpty match {
        case true => Some(ValidationMessage("description|embedUrl", MISSING_DESCRIPTION_OR_EMBED_URL))
        case false => None
      }
    }
  }
}
