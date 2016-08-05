package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.learningpathapi.model.api.{ValidationMessage, ValidationError}
import no.ndla.learningpathapi.model.domain.ValidationException
import org.mockito.Mockito._
class LanguageValidatorTest extends UnitSuite with TestEnvironment {

  var validator: LanguageValidator = _

  override def beforeEach() = {
    validator = new LanguageValidator
    resetMocks()
  }

  test("That LanguageValidator returns no error message for nb") {
    when(mappingApiClient.languageCodeSupported6391("nb")).thenReturn(true)
    validator.validate("path1.path2", Some("nb")) should be(None)
  }

  test("That LanguageValidator returns error for something") {
    when(mappingApiClient.languageCodeSupported6391("something")).thenReturn(false)
    val errorMessage = validator.validate("path1.path2", Some("something"))
    errorMessage.isDefined should be(right = true)
    errorMessage.get.field should equal("path1.path2")
    errorMessage.get.message should equal("Language 'something' is not a supported value.")
  }

  test("That exception is thrown when calling singleton object") {
    when(languageValidator.validate("language", Some("error"))).thenReturn(Some(ValidationMessage("language", "Language 'error' is not a supported value.")))

    assertResult("Language 'error' is not a supported value.") {
      intercept[ValidationException]{
        LanguageValidator.validate("language", Some("error"))
      }.errors.head.message
    }
  }

  test("That input value is returned when no error") {
    when(languageValidator.validate("language", Some("nb"))).thenReturn(None)
    LanguageValidator.validate("language", Some("nb")) should equal (Some("nb"))
  }
}
