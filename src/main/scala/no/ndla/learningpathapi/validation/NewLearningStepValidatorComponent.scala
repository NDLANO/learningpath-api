package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{NewLearningStep, ValidationMessage}

trait NewLearningStepValidatorComponent {
  this: TitleValidatorComponent with DescriptionValidatorComponent with EmbedContentValidatorComponent with StepTypeValidatorComponent with LicenseValidatorComponent =>
  val newLearningStepValidator: NewLearningStepValidator

  class NewLearningStepValidator {
    def validate(newLearningStep: NewLearningStep): List[ValidationMessage] = {
      titleValidator.validate(newLearningStep.title) :::
        basicHtmlDescriptionValidator.validate(newLearningStep.description) :::
        embedContentValidator.validate(newLearningStep.embedContent) :::
        stepTypeValidator.validate(newLearningStep.`type`).toList :::
        licenseValidator.validate(newLearningStep.license).toList
    }
  }
}
