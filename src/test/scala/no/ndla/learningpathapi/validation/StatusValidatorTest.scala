package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite

class StatusValidatorTest extends UnitSuite {

  test("That validate returns a ValidationMessage if the status is not valid") {
    val validationMessage = StatusValidator.validate("Invalid")
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("status")
    validationMessage.get.message should equal("'Invalid' is not a valid publishingstatus.")
  }

  test("That validate does not return a ValidationMessage if the status is valid") {
    StatusValidator.validate("PUBLISHED") should be (None)
  }
}
