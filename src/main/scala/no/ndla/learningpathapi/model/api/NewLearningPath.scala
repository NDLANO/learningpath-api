package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.model.domain.ValidationException
import no.ndla.learningpathapi.validation.NewLearningPathValidator
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a new learningpath")
case class NewLearningPath(@(ApiModelProperty @field)(description = "The titles of the learningpath") title:List[Title],
                            @(ApiModelProperty @field)(description = "The descriptions of the learningpath") description:List[Description],
                            @(ApiModelProperty @field)(description = "Url to cover-photo in NDLA image-api.") coverPhotoUrl:Option[String],
                            @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes. Must be greater than 0") duration:Option[Int],
                            @(ApiModelProperty @field)(description = "Searchable tags for the learningpath") tags:List[LearningPathTag]) {

  def validate(): NewLearningPath = {
    val validationResult = new NewLearningPathValidator().validate(this)
    validationResult.isEmpty match {
      case true => this
      case false => throw new ValidationException(errors = validationResult)
    }
  }
}
