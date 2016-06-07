package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.{ValidationMessage, Title}
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

import org.mockito.Mockito._
import org.mockito.Matchers._

class TitleValidatorTest extends UnitSuite {

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
    val validationErrors = validator.validate(List(DefaultTitle.copy(title = "<h1>Illegal text</h1>")))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("title.title")
  }

  test("That TitleValidator validates language") {
    val validationErrors = validator.validate(List(DefaultTitle.copy(language = Some("bergensk"))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("title.language")
  }

  test("That TitleValidator validates both title text and language") {
    val validationErrors = validator.validate(List(DefaultTitle.copy(title = "<h1>Illegal text</h1>", language = Some("bergensk"))))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("title.title")
    validationErrors.last.field should equal("title.language")

  }

  test("That TitleValidator returns no errors for a valid title") {
    validator.validate(List(DefaultTitle)) should equal(List())
  }

  test("That TitleValidator validates all titles") {
    val validationErrors = validator.validate(List(
      DefaultTitle.copy(title = "<h1>Invalid text</h1>"),
      DefaultTitle.copy(title = "<h1>Invalid text</h1>")
    ))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("title.title")
    validationErrors.last.field should equal("title.title")
  }

  test("That TitleValidator does not return error message when no titles are defined and no titles are required") {
    new TitleValidator(titleRequired = false).validate(List()) should equal(List())
  }

  test("That TitleValidator returns error message for an invalid title even if no titles are required") {
    val validationErrors = new TitleValidator(titleRequired = false).validate(List(DefaultTitle.copy(title = "<h1>Invalid text</h1>")))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("title.title")
  }
}
