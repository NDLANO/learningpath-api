package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{NewLearningPath, ValidationMessage}


trait NewLearningPathValidatorComponent {
  this: TitleValidatorComponent with DescriptionValidatorComponent with DurationValidatorComponent with CoverPhotoValidatorComponent with TagsValidatorComponent =>
  val newLearningPathValidator: NewLearningPathValidator

  class NewLearningPathValidator {
    def validate(newLearningPath: NewLearningPath): List[ValidationMessage] = {
      titleValidator.validate(newLearningPath.title) :::
      noHtmlDescriptionValidator.validate(newLearningPath.description) :::
      durationValidator.validate(newLearningPath.duration).toList :::
      coverPhotoValidator.validate(newLearningPath.coverPhotoUrl).toList :::
      tagsValidator.validate(newLearningPath.tags)
    }
  }
}
