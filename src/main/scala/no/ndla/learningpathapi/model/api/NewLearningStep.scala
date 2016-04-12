package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.model.domain.ValidationException
import no.ndla.learningpathapi.validation.NewLearningStepValidator
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about a new learningstep")
case class NewLearningStep(@(ApiModelProperty@field)(description = "The titles of the learningstep") title: List[Title],
                           @(ApiModelProperty@field)(description = "The descriptions of the learningstep") description: List[Description],
                           @(ApiModelProperty@field)(description = "The embed content for the learningstep") embedContent: List[EmbedContent],
                           @(ApiModelProperty@field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`: String,
                           @(ApiModelProperty@field)(description = "The license for this step. Must be plain text") license: Option[String]) {

  def validate(): NewLearningStep = {
    val validationResult = new NewLearningStepValidator().validate(this)
    validationResult.isEmpty match {
      case true => this
      case false => throw new ValidationException(errors = validationResult)
    }
  }
}