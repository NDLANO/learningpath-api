package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class DurationValidatorTest extends UnitSuite with TestEnvironment{

  var validator: DurationValidator = _
  override def beforeEach() = {
    validator = new DurationValidator
  }

  test("That validate returns error when duration less than 1") {
    val validationError = validator.validate(0)
    validationError.isDefined should be(right = true)
    validationError.get.field should equal("duration")
    validationError.get.message should equal("Required value duration must be greater than 0.")
  }

  test("That validate doesn't return an error when valid value") {
    validator.validate(1) should equal(None)
  }
}
