package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class StepTypeValidatorTest extends UnitSuite with TestEnvironment {

  var validator: StepTypeValidator = _

  override def beforeEach() = {
    validator = new StepTypeValidator
  }

  test("That empty stepType gives validation error"){
    val validationMessage = validator.validate("")
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("type")
    validationMessage.get.message should equal("'' is not a valid steptype.")
  }

  test("That unsupported stepType gives validation error"){
    val validationMessage = validator.validate("unsupported")
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("type")
    validationMessage.get.message should equal("'unsupported' is not a valid steptype.")
  }

  test("That valid stepType doesn't give validation error"){
    validator.validate("TEXT") should be (None)
  }
}
