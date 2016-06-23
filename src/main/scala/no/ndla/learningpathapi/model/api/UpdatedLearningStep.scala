package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.model.domain.ValidationException
import no.ndla.learningpathapi.validation.LearningStepValidator
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about a new learningstep")
case class UpdatedLearningStep(@(ApiModelProperty@field)(description = "The revision number for this learningstep") revision: Int,
                               @(ApiModelProperty@field)(description = "The titles of the learningstep") title: Seq[Title],
                               @(ApiModelProperty@field)(description = "The descriptions of the learningstep") description: Seq[Description],
                               @(ApiModelProperty@field)(description = "The embed content for the learningstep") embedContent: Seq[EmbedContent],
                               @(ApiModelProperty@field)(description = "Determines if the title of the step should be displayed in viewmode") showTitle: Option[Boolean],
                               @(ApiModelProperty@field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`: Option[String],
                               @(ApiModelProperty@field)(description = "The license for this step. Must be plain text") license: Option[String])