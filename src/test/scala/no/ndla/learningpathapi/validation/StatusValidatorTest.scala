package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class StatusValidatorTest extends UnitSuite with TestEnvironment {

  var validator: StatusValidator = _
  override def beforeEach() = {
    validator = new StatusValidator
  }

  test("That validate returns a ValidationMessage if the status is not valid") {
    val validationMessage = validator.validate("Invalid")
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("status")
    validationMessage.get.message should equal("'Invalid' is not a valid publishingstatus.")
  }

  test("That validate does not return a ValidationMessage if the status is valid") {
    validator.validate("PUBLISHED") should be (None)
  }
}
