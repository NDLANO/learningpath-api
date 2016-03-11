package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.{ValidationMessage, Title}
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

import org.mockito.Mockito._
import org.mockito.Matchers._

class TitleValidatorTest extends UnitSuite with TestEnvironment{

  var validator: TitleValidator = _

  override def beforeEach() = {
    validator = new TitleValidator
  }

  val DefaultTitle = Title("Some title", Some("nb"))

  test("That TitleValidator.validate returns error message when no titles are defined") {
    val errorMessages = validator.validate(List())
    errorMessages.size should be (1)
    errorMessages.head.field should equal("title")
    errorMessages.head.message should equal("At least one title is required.")
  }

  test("That TitleValidator validates title text") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.path2", "Invalid text")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationErrors = validator.validate(List(DefaultTitle))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("path1.path2")
    validationErrors.head.message should equal("Invalid text")
  }

  test("That TitleValidator validates language") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("path1.path2", "Invalid language")))

    val validationErrors = validator.validate(List(DefaultTitle))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("path1.path2")
    validationErrors.head.message should equal("Invalid language")
  }

  test("That TitleValidator validates both title text and language") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.title", "Invalid text")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("path1.language", "Invalid language")))

    val validationErrors = validator.validate(List(DefaultTitle))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("path1.title")
    validationErrors.head.message should equal("Invalid text")
    validationErrors.last.field should equal("path1.language")
    validationErrors.last.message should equal("Invalid language")

  }

  test("That TitleValidator returns no errors for a valid title") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)
    validator.validate(List(DefaultTitle)) should equal(List())
  }

  test("That TitleValidator validates all titles") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.title", "Invalid text")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationErrors = validator.validate(List(DefaultTitle, DefaultTitle))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("path1.title")
    validationErrors.head.message should equal("Invalid text")
    validationErrors.last.field should equal("path1.title")
    validationErrors.last.message should equal("Invalid text")
  }

}
