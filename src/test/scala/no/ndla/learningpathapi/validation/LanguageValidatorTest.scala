/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

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
    validator.validate("path1.path2", "nb", false) should be(None)
  }

  test("That LanguageValidator returns error for something") {
    val errorMessage = validator.validate("path1.path2", "something", false)
    errorMessage.isDefined should be(true)
    errorMessage.get.field should equal("path1.path2")
    errorMessage.get.message should equal("Language 'something' is not a supported value.")
  }

  test("That exception is thrown when calling singleton object") {
    when(languageValidator.validate("language", "error", false)).thenReturn(Some(ValidationMessage("language", "Language 'error' is not a supported value.")))

    assertResult("Language 'error' is not a supported value.") {
      intercept[ValidationException]{
        LanguageValidator.validate("language", "error")
      }.errors.head.message
    }
  }

  test("That input value is returned when no error") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    LanguageValidator.validate("language", "nb") should equal ("nb")
  }
}
