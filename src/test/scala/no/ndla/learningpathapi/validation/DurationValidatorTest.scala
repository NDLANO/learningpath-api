package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class DurationValidatorTest extends UnitSuite with TestEnvironment{

  var validator: DurationValidator = _
  override def beforeEach() = {
    validator = new DurationValidator
  }

  test("That validate returns error when duration less than 1") {
    val validationError = validator.validate(Some(0))
    validationError.isDefined should be(right = true)
    validationError.get.field should equal("duration")
    validationError.get.message should equal("Required value duration must be greater than 0.")
  }

  test("That validate doesn't return an error when valid value") {
    validator.validate(Some(1)) should equal(None)
  }

  test("That validate accepts None") {
    validator.validate(None) should equal (None)
  }

  test("That validateRequired doesn't accept None") {
    val validationError = validator.validateRequired(None)
    validationError.isDefined should be (right = true)
    validationError.get.field should equal ("duration")
    validationError.get.message should equal ("Required value is empty.")
  }
}
