/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite

class StatusValidatorTest extends UnitSuite {

  var validator: StatusValidator = _
  override def beforeEach() = {
    validator = new StatusValidator
  }

  test(
    "That validateLearningPathStatus returns a ValidationMessage if the status is not valid") {
    val validationMessage = validator.validateLearningPathStatus("Invalid")
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("status")
    validationMessage.get.message should equal(
      "'Invalid' is not a valid publishingstatus.")
  }

  test(
    "That validateLearningPathStatus does not return a ValidationMessage if the status is valid") {
    validator.validateLearningPathStatus("PUBLISHED") should be(None)
  }

  test(
    "That validateLearningStepStatus returns a ValidationMessage if the status is not valid") {
    val validationMessage = validator.validateLearningStepStatus("Invalid")
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("status")
    validationMessage.get.message should equal(
      "'Invalid' is not a valid status.")
  }

  test(
    "That validateLearningStepStatus does not return a ValidationMessage if the status is valid") {
    validator.validateLearningStepStatus("DELETED") should be(None)
  }
}
