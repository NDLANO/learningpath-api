/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Summary of meta information for a learningstep")
case class LearningStepSummaryV2(
    @(ApiModelProperty @field)(description = "The id of the learningstep") id: Long,
    @(ApiModelProperty @field)(description = "The sequence number for the step. The first step has seqNo 0.") seqNo: Int,
    @(ApiModelProperty @field)(description = "The title of the learningstep") title: Title,
    @(ApiModelProperty @field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`: String,
    @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningstep can be found") metaUrl: String
)
