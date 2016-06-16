package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.model.domain.ValidationException
import no.ndla.learningpathapi.validation.StatusValidator
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field


@ApiModel(description = "Status information about a learningpath")
case class LearningStepStatus(@(ApiModelProperty @field)(description = "The status of the learningstep", allowableValues = "ACTIVE,DELETED") status:String) {
  def validate() = {
    new StatusValidator().validateLearningStepStatus(status) match {
      case None => this
      case Some(result) => throw new ValidationException(errors = List(result))
    }
  }
}