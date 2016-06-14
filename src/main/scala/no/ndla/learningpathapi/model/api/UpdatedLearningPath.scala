package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.model.domain.ValidationException
import no.ndla.learningpathapi.validation.LearningPathValidator
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a new learningpath")
case class UpdatedLearningPath(@(ApiModelProperty@field)(description = "The revision number for this learningpath") revision: Int,
                               @(ApiModelProperty@field)(description = "The titles of the learningpath") title: Seq[Title],
                               @(ApiModelProperty@field)(description = "The descriptions of the learningpath") description: Seq[Description],
                               @(ApiModelProperty@field)(description = "Url to cover-photo in NDLA image-api.") coverPhotoUrl: Option[String],
                               @(ApiModelProperty@field)(description = "The duration of the learningpath in minutes. Must be greater than 0") duration: Option[Int],
                               @(ApiModelProperty@field)(description = "Searchable tags for the learningpath") tags: Seq[LearningPathTag]) {

  def validate(): UpdatedLearningPath = {
    val validationResult = new LearningPathValidator().validate(this)
    validationResult.isEmpty match {
      case true => this
      case false => throw new ValidationException(errors = validationResult)
    }
  }
}
