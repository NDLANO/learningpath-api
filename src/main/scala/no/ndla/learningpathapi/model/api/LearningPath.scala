package no.ndla.learningpathapi.model.api

import java.util.Date

import no.ndla.learningpathapi.model.domain
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a learningpath")
case class LearningPath(@(ApiModelProperty@field)(description = "The unique id of the learningpath") id: Long,
                        @(ApiModelProperty@field)(description = "The revision number for this learningpath") revision: Int,
                        @(ApiModelProperty@field)(description = "The id this learningpath is based on, if any") isBasedOn: Option[Long],
                        @(ApiModelProperty@field)(description = "The titles of the learningpath") title: Seq[Title],
                        @(ApiModelProperty@field)(description = "The descriptions of the learningpath") description: Seq[Description],
                        @(ApiModelProperty@field)(description = "The full url to where the complete metainformation about the learningpath can be found") metaUrl: String,
                        @(ApiModelProperty@field)(description = "The learningsteps-summaries for this learningpath") learningsteps: Seq[LearningStepSummary],
                        @(ApiModelProperty@field)(description = "The full url to where the learningsteps can be found") learningstepUrl: String,
                        @(ApiModelProperty@field)(description = "Url to where a cover photo can be found") coverPhotoUrl: Option[String],
                        @(ApiModelProperty@field)(description = "The duration of the learningpath in minutes") duration: Option[Int],
                        @(ApiModelProperty@field)(description = "The publishing status of the learningpath", allowableValues = "PUBLISHED,PRIVATE,NOT_LISTED") status: String,
                        @(ApiModelProperty@field)(description = "Verification status", allowableValues = "CREATED_BY_NDLA,VERIFIED_BY_NDLA,EXTERNAL") verificationStatus: String,
                        @(ApiModelProperty@field)(description = "The date when this learningpath was last updated.") lastUpdated: Date,
                        @(ApiModelProperty@field)(description = "Searchable tags for the learningpath") tags: Seq[LearningPathTag],
                        @(ApiModelProperty@field)(description = "The author of this learningpath") author: Author,
                        @(ApiModelProperty@field)(description = "True if authenticated user may edit this learningpath") canEdit: Boolean) {

  def isPrivate: Boolean = {
    status == domain.LearningPathStatus.PRIVATE.toString
  }
}
