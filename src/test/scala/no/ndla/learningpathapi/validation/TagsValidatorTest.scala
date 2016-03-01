package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.validation.TagsValidator.validate
import no.ndla.learningpathapi.{LearningPathTag, UnitSuite}

class TagsValidatorTest extends UnitSuite {

  val VALID_TAG_WITHOUT_LANGUAGE = LearningPathTag("TAG", None)
  val VALID_TAG_WITH_VALID_LANGUAGE = LearningPathTag("TAG", Some("nb"))
  val VALID_TAG_WITH_INVALID_LANGUAGE = LearningPathTag("TAG", Some("unsupported"))
  
  val INVALID_TAG_WITHOUT_LANGUAGE = LearningPathTag("", None)
  val INVALID_TAG_WITH_VALID_LANGUAGE = LearningPathTag("", Some("nb"))
  val INVALID_TAG_WITH_INVALID_LANGUAGE = LearningPathTag("", Some("unsupported"))

  test("That no tags gives no error") {
    validate(List()) should equal(List())
  }
  
  test("That one valid tag without language gives no error") {
    validate(List(VALID_TAG_WITHOUT_LANGUAGE)) should equal(List())
  }
  
  test("That one valid tag with valid language gives no error") {
    validate(List(VALID_TAG_WITH_VALID_LANGUAGE)) should equal(List())
  }

  test("That one invalid tag without language gives correct error") {
    val validationErrors = validate(List(INVALID_TAG_WITHOUT_LANGUAGE))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("tags.tag")
    validationErrors.head.message should equal("Required value tag is empty.")
  }

  test("That one invalid tag with valid language gives correct error") {
    val validationErrors = validate(List(INVALID_TAG_WITH_VALID_LANGUAGE))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("tags.tag")
    validationErrors.head.message should equal("Required value tag is empty.")
  }

  test("That one invalid tag with invalid language reports both errors") {
    val errors = validate(List(INVALID_TAG_WITH_INVALID_LANGUAGE))
    errors.size should be(2)
    errors.head.field should equal("tags.tag")
    errors.head.message should equal("Required value tag is empty.")
    errors.last.field should equal("tags.language")
    errors.last.message should equal("Language 'unsupported' is not a supported value.")
  }

  test("That two invalid tags gives correct error message") {
    val errors = validate(List(VALID_TAG_WITH_INVALID_LANGUAGE, INVALID_TAG_WITH_VALID_LANGUAGE))
    errors.size should be(2)

    errors.head.field should equal("tags.language")
    errors.head.message should equal("Language 'unsupported' is not a supported value.")
    errors.last.field should equal("tags.tag")
    errors.last.message should equal("Required value tag is empty.")
  }
}
