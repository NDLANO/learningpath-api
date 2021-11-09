/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

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

}
