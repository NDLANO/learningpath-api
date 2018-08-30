/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain.{LearningPathStatus, StepStatus}

class StatusValidator {
  def validateLearningStepStatus(status: String): Option[ValidationMessage] = {
    StepStatus.valueOf(status).isEmpty match {
      case true =>
        Some(ValidationMessage("status", s"'$status' is not a valid status."))
      case false => None
    }
  }

  def validateLearningPathStatus(status: String): Option[ValidationMessage] = {
    LearningPathStatus.valueOf(status).isEmpty match {
      case true =>
        Some(
          ValidationMessage("status",
                            s"'$status' is not a valid publishingstatus."))
      case false => None
    }
  }
}
