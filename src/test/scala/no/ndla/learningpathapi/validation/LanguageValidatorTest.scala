package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite
import no.ndla.learningpathapi.model.api.ValidationError
import no.ndla.learningpathapi.model.domain.ValidationException

class LanguageValidatorTest extends UnitSuite {

  var validator: LanguageValidator = _

  override def beforeEach() = {
    validator = new LanguageValidator
  }

  test("That LanguageValidator returns no error message for nb") {
    validator.validate("path1.path2", Some("nb")) should be(None)
  }

  test("That LanguageValidator returns error for something") {
    val errorMessage = validator.validate("path1.path2", Some("something"))
    errorMessage.isDefined should be(right = true)
    errorMessage.get.field should equal("path1.path2")
    errorMessage.get.message should equal("Language 'something' is not a supported value.")
  }

  test("That exception is thrown when calling singleton object") {
    assertResult("Language 'error' is not a supported value.") {
      intercept[ValidationException]{
        LanguageValidator.validate("language", Some("error"))
      }.errors.head.message
    }
  }

  test("That input value is returned when no error") {
    LanguageValidator.validate("language", Some("nb")) should equal (Some("nb"))
  }
}
