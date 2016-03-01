package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite

class StepTypeValidatorTest extends UnitSuite {

  test("That empty stepType gives validation error"){
    val validationMessage = StepTypeValidator.validate("")
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("type")
    validationMessage.get.message should equal("'' is not a valid steptype.")
  }

  test("That unsupported stepType gives validation error"){
    val validationMessage = StepTypeValidator.validate("unsupported")
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("type")
    validationMessage.get.message should equal("'unsupported' is not a valid steptype.")
  }

  test("That valid stepType doesn't give validation error"){
    StepTypeValidator.validate("TEXT") should be (None)
  }
}
