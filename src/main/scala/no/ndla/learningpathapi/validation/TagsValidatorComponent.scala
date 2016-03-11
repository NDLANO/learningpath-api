package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.{LearningPathTag, ValidationMessage}

trait TagsValidatorComponent {
  this: TextValidatorComponent with LanguageValidatorComponent =>
  val tagsValidator: TagsValidator

  class TagsValidator {

    def validate(tags: List[LearningPathTag]): List[ValidationMessage] = {
      tags.flatMap(tag => validate(tag))
    }

    def validate(tag: LearningPathTag): List[ValidationMessage] = {
      noHtmlTextValidator.validate("tags.tag", tag.tag).toList :::
      languageValidator.validate("tags.language", tag.language).toList
    }

  }
}
