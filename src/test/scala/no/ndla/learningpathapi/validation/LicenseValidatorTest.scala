package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite

class LicenseValidatorTest extends UnitSuite {

  test("That an emtpy license gives an error"){
    val validationMessage = LicenseValidator.validate(Some(""))
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("license")
    validationMessage.get.message should equal("Required value license is empty.")
  }

  test("That a license with html-characters gives an error") {
    val validationMessage = LicenseValidator.validate(Some("<strong>Invalid license</strong>"))
    validationMessage.isDefined should be (right = true)
    validationMessage.get.field should equal("license")
  }

  test("That None doesn't give an error"){
    LicenseValidator.validate(None) should be (None)
  }

  test("That valid license doesn't give an error"){
    LicenseValidator.validate(Some("A random license")) should be (None)
  }

}
