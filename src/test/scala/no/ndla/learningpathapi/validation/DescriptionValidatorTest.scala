package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{ValidationMessage, Description, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._

class DescriptionValidatorTest extends UnitSuite with TestEnvironment {

  var noHtmlValidator: DescriptionValidator = _
  var basicHtmlValidator: DescriptionValidator = _

  override def beforeEach() = {
    noHtmlValidator = new DescriptionValidator(allowHtml = false)
    basicHtmlValidator = new DescriptionValidator(allowHtml = true)
  }

  val DefaultDescription = Description("Some description", Some("nb"))
  
  test("That DescriptionValidator.validate returns error message when no descriptions are defined") {
    val errorMessages = noHtmlValidator.validate(List())
    errorMessages.size should be (1)
    errorMessages.head.field should equal("description")
    errorMessages.head.message should equal("At least one description is required.")
  }

  test("That DescriptionValidator validates description text") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.path2", "Invalid text")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationErrors = noHtmlValidator.validate(List(DefaultDescription))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("path1.path2")
    validationErrors.head.message should equal("Invalid text")
  }

  test("That DescriptionValidator validates language") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("path1.path2", "Invalid language")))

    val validationErrors = noHtmlValidator.validate(List(DefaultDescription))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("path1.path2")
    validationErrors.head.message should equal("Invalid language")
  }

  test("That DescriptionValidator validates both description text and language") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.description", "Invalid text")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("path1.language", "Invalid language")))

    val validationErrors = noHtmlValidator.validate(List(DefaultDescription))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("path1.description")
    validationErrors.head.message should equal("Invalid text")
    validationErrors.last.field should equal("path1.language")
    validationErrors.last.message should equal("Invalid language")

  }

  test("That DescriptionValidator returns no errors for a valid description") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)
    noHtmlValidator.validate(List(DefaultDescription)) should equal(List())
  }

  test("That DescriptionValidator validates all descriptions") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.description", "Invalid text")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationErrors = noHtmlValidator.validate(List(DefaultDescription, DefaultDescription))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("path1.description")
    validationErrors.head.message should equal("Invalid text")
    validationErrors.last.field should equal("path1.description")
    validationErrors.last.message should equal("Invalid text")
  }

  test("That basicHtmlValidator validates uses basicHtmlTextValidator") {
    when(basicHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.description", "Invalid text")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationErrors = basicHtmlValidator.validate(List(DefaultDescription, DefaultDescription))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("path1.description")
    validationErrors.head.message should equal("Invalid text")
    validationErrors.last.field should equal("path1.description")
    validationErrors.last.message should equal("Invalid text")
  }
}
