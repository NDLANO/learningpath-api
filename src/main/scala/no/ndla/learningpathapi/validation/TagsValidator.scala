package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.LearningPathTag
import no.ndla.learningpathapi.model.ValidationMessage

object TagsValidator {
  val TAG_EMPTY = "Required value tag is empty."

  def validate(tags: List[LearningPathTag]): List[ValidationMessage] = {
    tags.flatMap(tag => validate(tag))
  }

  def validate(tag: LearningPathTag): List[ValidationMessage] = {
    val tagResult = if (tag.tag.isEmpty) Some(ValidationMessage("tags.tag", TAG_EMPTY)) else None
    List(tagResult, tag.language.flatMap(tag => LanguageValidator.validate("tags.language", tag))).flatMap(nonEmpty => nonEmpty)
  }
}
