package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite

class DurationValidatorTest extends UnitSuite {

  test("That validate returns error when duration less than 1") {
    val validationError = DurationValidator.validate(0)
    validationError.isDefined should be(right = true)
    validationError.get.field should equal("duration")
    validationError.get.message should equal("Required value duration must be greater than 0.")
  }

  test("That validate doesn't return an error when valid value") {
    DurationValidator.validate(1) should equal(None)
  }
}
