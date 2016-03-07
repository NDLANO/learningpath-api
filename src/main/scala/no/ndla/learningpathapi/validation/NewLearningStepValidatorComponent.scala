package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{NewLearningStep, ValidationMessage}

trait NewLearningStepValidatorComponent {
  this: TitleValidatorComponent with DescriptionValidatorComponent with EmbedUrlValidatorComponent with StepTypeValidatorComponent with LicenseValidatorComponent =>
  val newLearningStepValidator: NewLearningStepValidator

  class NewLearningStepValidator {
    def validate(newLearningStep: NewLearningStep): List[ValidationMessage] = {
      titleValidator.validate(newLearningStep.title) :::
        basicHtmlDescriptionValidator.validate(newLearningStep.description) :::
        embedUrlValidator.validate(newLearningStep.embedUrl) :::
        stepTypeValidator.validate(newLearningStep.`type`).toList :::
        licenseValidator.validate(newLearningStep.license).toList
    }
  }
}
