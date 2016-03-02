package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{Title, LearningPathTag, ValidationMessage}

object TagsValidator {
  val TAG_EMPTY = "Required value tag is empty."

  def validate(tags: List[LearningPathTag]): List[ValidationMessage] = {
    tags.flatMap(tag => validate(tag))
  }

  def validate(tag: LearningPathTag): List[ValidationMessage] = {
    validateTagText(tag.tag).toList :::
    validateTagLanguage(tag.language).toList
  }

  def validateTagText(text: String): Option[ValidationMessage] = {
    text.isEmpty match {
      case true => Some(ValidationMessage("tags.tag", TAG_EMPTY))
      case false => TextValidator.validateNoHtmlTags("tags.tag", text)
    }
  }

  def validateTagLanguage(language: Option[String]): Option[ValidationMessage] = {
    language.flatMap(language => LanguageValidator.validate("tags.language", language))
  }
}
