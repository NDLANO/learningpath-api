/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import java.util.Date

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Summary of meta information for a learningpath")
case class LearningPathSummaryV2(
    @(ApiModelProperty @field)(description = "The unique id of the learningpath") id: Long,
    @(ApiModelProperty @field)(description = "The revision number for this learningpath") revision: Option[Int],
    @(ApiModelProperty @field)(description = "The titles of the learningpath") title: Title,
    @(ApiModelProperty @field)(description = "The descriptions of the learningpath") description: Description,
    @(ApiModelProperty @field)(description = "The introductions of the learningpath") introduction: Introduction,
    @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningpath can be found") metaUrl: String,
    @(ApiModelProperty @field)(description = "Url to where a cover photo can be found") coverPhotoUrl: Option[String],
    @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes") duration: Option[Int],
    @(ApiModelProperty @field)(description = "The publishing status of the learningpath.", allowableValues = "PUBLISHED,PRIVATE,UNLISTED,SUBMITTED") status: String,
    @(ApiModelProperty @field)(description = "The date when this learningpath was last updated.") lastUpdated: Date,
    @(ApiModelProperty @field)(description = "Searchable tags for the learningpath") tags: LearningPathTags,
    @(ApiModelProperty @field)(description = "The contributors of this learningpath") copyright: Copyright,
    @(ApiModelProperty @field)(description = "A list of available languages for this audio") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "The id this learningpath is based on, if any") isBasedOn: Option[Long],
    @(ApiModelProperty @field)(description = "Message that admins can place on a LearningPath for notifying a owner of issues with the LearningPath") message: Option[String]
)
