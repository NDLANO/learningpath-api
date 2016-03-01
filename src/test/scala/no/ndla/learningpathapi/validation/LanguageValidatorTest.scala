package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite

class LanguageValidatorTest extends UnitSuite {

  test("That LanguageValidator returns no error message for nb") {
    LanguageValidator.validate("tag", "nb") should be(None)
  }

  test("That LanguageValidator returns error for something") {
    val errorMessage = LanguageValidator.validate("tag", "something")
    errorMessage.isDefined should be(right = true)
    errorMessage.get.field should equal("tag")
    errorMessage.get.message should equal("Language 'something' is not a supported value.")
  }

}
