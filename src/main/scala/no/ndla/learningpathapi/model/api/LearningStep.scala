package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about a learningstep")
case class LearningStep(@(ApiModelProperty@field)(description = "The id of the learningstep") id: Long,
                        @(ApiModelProperty@field)(description = "The sequence number for the step. The first step has seqNo 0.") seqNo: Int,
                        @(ApiModelProperty@field)(description = "The titles of the learningstep") title: List[Title],
                        @(ApiModelProperty@field)(description = "The descriptions of the learningstep") description: List[Description],
                        @(ApiModelProperty@field)(description = "The embed content for the learningstep") embedContent: List[EmbedContent],
                        @(ApiModelProperty@field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`: String,
                        @(ApiModelProperty@field)(description = "The license for this step.") license: Option[String],
                        @(ApiModelProperty@field)(description = "The full url to where the complete metainformation about the learningstep can be found") metaUrl: String,
                        @(ApiModelProperty@field)(description = "True if authenticated user may edit this learningstep") canEdit: Boolean)
