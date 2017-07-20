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

@ApiModel(description = "Information about a learningstep")
case class LearningStep(@(ApiModelProperty@field)(description = "The id of the learningstep") id: Long,
                        @(ApiModelProperty@field)(description = "The revision number for this learningstep") revision: Int,
                        @(ApiModelProperty@field)(description = "The sequence number for the step. The first step has seqNo 0.") seqNo: Int,
                        @(ApiModelProperty@field)(description = "The titles of the learningstep") title: Seq[Title],
                        @(ApiModelProperty@field)(description = "The descriptions of the learningstep") description: Seq[Description],
                        @(ApiModelProperty@field)(description = "The embed content for the learningstep") embedUrl: Seq[EmbedUrl],
                        @(ApiModelProperty@field)(description = "Determines if the title of the step should be displayed in viewmode") showTitle: Boolean,
                        @(ApiModelProperty@field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`: String,
                        @(ApiModelProperty@field)(description = "Describes the copyright information for the learningstep") license: Option[License],
                        @(ApiModelProperty@field)(description = "The full url to where the complete metainformation about the learningstep can be found") metaUrl: String,
                        @(ApiModelProperty@field)(description = "True if authenticated user may edit this learningstep") canEdit: Boolean,
                        @(ApiModelProperty@field)(description = "The status of the learningstep", allowableValues = "ACTIVE,DELETED") status: String)

@ApiModel(description = "Information about a learningstep")
case class LearningStepV2(@(ApiModelProperty@field)(description = "The id of the learningstep") id: Long,
                          @(ApiModelProperty@field)(description = "The revision number for this learningstep") revision: Int,
                          @(ApiModelProperty@field)(description = "The sequence number for the step. The first step has seqNo 0.") seqNo: Int,
                          @(ApiModelProperty@field)(description = "The title of the learningstep") title: String,
                          @(ApiModelProperty@field)(description = "The language of the learningstep. Default is 'nb'") language: String,
                          @(ApiModelProperty@field)(description = "The description of the learningstep") description: Option[String],
                          @(ApiModelProperty@field)(description = "The embed content for the learningstep") embedUrl: Option[EmbedUrlV2],
                          @(ApiModelProperty@field)(description = "Determines if the title of the step should be displayed in viewmode") showTitle: Boolean,
                          @(ApiModelProperty@field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`: String,
                          @(ApiModelProperty@field)(description = "Describes the copyright information for the learningstep") license: Option[License],
                          @(ApiModelProperty@field)(description = "The full url to where the complete metainformation about the learningstep can be found") metaUrl: String,
                          @(ApiModelProperty@field)(description = "True if authenticated user may edit this learningstep") canEdit: Boolean,
                          @(ApiModelProperty@field)(description = "The status of the learningstep", allowableValues = "ACTIVE,DELETED") status: String,
                          @(ApiModelProperty@field)(description = "The supported languages of the learningstep") supportedLanguages: Seq[String])
