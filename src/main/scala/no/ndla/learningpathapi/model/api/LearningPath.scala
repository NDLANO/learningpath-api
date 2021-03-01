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

import java.util.Date
import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information for a learningpath")
case class LearningPathV2(
    @(ApiModelProperty @field)(description = "The unique id of the learningpath") id: Long,
    @(ApiModelProperty @field)(description = "The revision number for this learningpath") revision: Int,
    @(ApiModelProperty @field)(description = "The id this learningpath is based on, if any") isBasedOn: Option[Long],
    @(ApiModelProperty @field)(description = "The title of the learningpath") title: Title,
    @(ApiModelProperty @field)(description = "The description of the learningpath") description: Description,
    @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningpath can be found") metaUrl: String,
    @(ApiModelProperty @field)(description = "The learningsteps-summaries for this learningpath") learningsteps: Seq[LearningStepV2],
    @(ApiModelProperty @field)(description = "The full url to where the learningsteps can be found") learningstepUrl: String,
    @(ApiModelProperty @field)(description = "Information about where the cover photo can be found") coverPhoto: Option[CoverPhoto],
    @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes") duration: Option[Int],
    @(ApiModelProperty @field)(description = "The publishing status of the learningpath", allowableValues = "PUBLISHED,PRIVATE,UNLISTED,SUBMITTED") status: String,
    @(ApiModelProperty @field)(description = "Verification status", allowableValues = "CREATED_BY_NDLA,VERIFIED_BY_NDLA,EXTERNAL") verificationStatus: String,
    @(ApiModelProperty @field)(description = "The date when this learningpath was last updated.") lastUpdated: Date,
    @(ApiModelProperty @field)(description = "Searchable tags for the learningpath") tags: LearningPathTags,
    @(ApiModelProperty @field)(description = "Describes the copyright information for the learningpath") copyright: Copyright,
    @(ApiModelProperty @field)(description = "True if authenticated user may edit this learningpath") canEdit: Boolean,
    @(ApiModelProperty @field)(description = "The supported languages for this learningpath") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "Visible if administrator or owner of LearningPath") ownerId: Option[String],
    @(ApiModelProperty @field)(description = "Message set by administrator. Visible if administrator or owner of LearningPath") message: Option[Message]
)
