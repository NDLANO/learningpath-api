package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.ComponentRegistry
import no.ndla.learningpathapi.model.domain.ValidationException
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Status information about a learningpath")
case class LearningPathStatus(@(ApiModelProperty @field)(description = "The publishing status of the learningpath", allowableValues = "PUBLISHED,PRIVATE,NOT_LISTED") status:String) {
  def validate() = {
    ComponentRegistry.statusValidator.validate(status) match {
      case None => this
      case Some(result) => throw new ValidationException(errors = List(result))
    }
  }
}