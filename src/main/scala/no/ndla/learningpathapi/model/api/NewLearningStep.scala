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

@ApiModel(description = "Information about a new learningstep")
case class NewLearningStepV2(@(ApiModelProperty@field)(description = "The titles of the learningstep") title: String,
                             @(ApiModelProperty@field)(description = "The descriptions of the learningstep") description: Option[String],
                             @(ApiModelProperty@field)(description = "The chosen language") language: String,
                             @(ApiModelProperty@field)(description = "The embed content for the learningstep") embedUrl: Option[EmbedUrlV2],
                             @(ApiModelProperty@field)(description = "Determines if the title of the step should be displayed in viewmode") showTitle: Boolean,
                             @(ApiModelProperty@field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`: String,
                             @(ApiModelProperty@field)(description = "Describes the copyright information for the learningstep") license: Option[String])