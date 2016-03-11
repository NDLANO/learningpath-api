package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.{ValidationMessage, LearningPathTag}
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._

class TagsValidatorTest extends UnitSuite with TestEnvironment {

  val DefaultTag = LearningPathTag("Some tag", Some("nb"))

  var validator: TagsValidator = _
  override def beforeEach() = {
    validator = new TagsValidator
  }

  test("That TagsValidator validates tag text") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.path2", "Invalid tag")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationErrors = validator.validate(List(DefaultTag))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("path1.path2")
    validationErrors.head.message should equal("Invalid tag")
  }

  test("That TagsValidator validates language") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("path1.path2", "Invalid language")))

    val validationErrors = validator.validate(List(DefaultTag))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("path1.path2")
    validationErrors.head.message should equal("Invalid language")
  }

  test("That TagsValidator validates both tag text and language") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.tag", "Invalid tag")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("path1.language", "Invalid language")))

    val validationErrors = validator.validate(List(DefaultTag))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("path1.tag")
    validationErrors.head.message should equal("Invalid tag")
    validationErrors.last.field should equal("path1.language")
    validationErrors.last.message should equal("Invalid language")

  }

  test("That TagsValidator returns no errors for a valid tag") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)
    validator.validate(List(DefaultTag)) should equal(List())
  }

  test("That TagsValidator validates all tags") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.tag", "Invalid tag")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationErrors = validator.validate(List(DefaultTag, DefaultTag))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("path1.tag")
    validationErrors.head.message should equal("Invalid tag")
    validationErrors.last.field should equal("path1.tag")
    validationErrors.last.message should equal("Invalid tag")
  }
}
