package no.ndla.learningpathapi

import java.text.SimpleDateFormat
import java.util.Date

import no.ndla.learningpathapi.model.ValidationException
import no.ndla.learningpathapi.validation._
import org.scalatra.swagger.ResponseMessage
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a learningpath")
case class LearningPath(
  @(ApiModelProperty @field)(description = "The unique id of the learningpath") id:Long,
  @(ApiModelProperty @field)(description = "The titles of the learningpath") title:List[Title],
  @(ApiModelProperty @field)(description = "The descriptions of the learningpath") description:List[Description],
  @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningpath can be found") metaUrl:String,
  @(ApiModelProperty @field)(description = "The learningsteps for this learningpath") learningsteps:List[LearningStep],
  @(ApiModelProperty @field)(description = "The full url to where the learningsteps can be found") learningstepUrl:String,
  @(ApiModelProperty @field)(description = "Url to where a cover photo can be found") coverPhotoUrl:Option[String],
  @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes") duration:Int,
  @(ApiModelProperty @field)(description = "The publishing status of the learningpath", allowableValues = "PUBLISHED,PRIVATE,NOT_LISTED") status:String,
  @(ApiModelProperty @field)(description = "Verification status", allowableValues = "CREATED_BY_NDLA,VERIFIED_BY_NDLA,EXTERNAL") verificationStatus:String,
  @(ApiModelProperty @field)(description = "The date when this learningpath was last updated.") lastUpdated:Date,
  @(ApiModelProperty @field)(description = "Searchable tags for the learningpath") tags:List[LearningPathTag],
  @(ApiModelProperty @field)(description = "The author of this learningpath") author:Author
) {
  def isPrivate: Boolean = {
    status == model.LearningPathStatus.PRIVATE.toString
  }
}

@ApiModel(description = "Meta information for a new learningpath")
case class NewLearningPath(
  @(ApiModelProperty @field)(description = "The titles of the learningpath") title:List[Title],
  @(ApiModelProperty @field)(description = "The descriptions of the learningpath") description:List[Description],
  @(ApiModelProperty @field)(description = "Url to cover-photo in NDLA image-api.") coverPhotoUrl:Option[String],
  @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes. Must be greater than 0") duration:Int,
  @(ApiModelProperty @field)(description = "Searchable tags for the learningpath") tags:List[LearningPathTag]
) {
  def validate(): NewLearningPath = {
    val validationResult = ComponentRegistry.newLearningPathValidator.validate(this)
    validationResult.isEmpty match {
      case true => this
      case false => throw new ValidationException(errors = validationResult)
    }
  }
}

@ApiModel(description = "Status information about a learningpath")
case class LearningPathStatus(
  @(ApiModelProperty @field)(description = "The publishing status of the learningpath", allowableValues = "PUBLISHED,PRIVATE,NOT_LISTED") status:String
) {
  def validate() = {
    ComponentRegistry.statusValidator.validate(status) match {
      case None => this
      case Some(result) => throw new ValidationException(errors = List(result))
    }
  }
}

@ApiModel(description = "Information about search-results")
case class SearchResult(
  @(ApiModelProperty @field)(description = "The total number of learningpaths matching this query") totalCount:Long,
  @(ApiModelProperty @field)(description = "For which page results are shown from") page:Int,
  @(ApiModelProperty @field)(description = "The number of results per page") pageSize:Int,
  @(ApiModelProperty @field)(description = "The search results") results:Iterable[LearningPathSummary]
)

@ApiModel(description = "Summary of meta information for a learningpath")
case class LearningPathSummary(
  @(ApiModelProperty @field)(description = "The unique id of the learningpath") id:Long,
  @(ApiModelProperty @field)(description = "The titles of the learningpath") title:List[Title],
  @(ApiModelProperty @field)(description = "The descriptions of the learningpath") description:List[Description],
  @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningpath can be found") metaUrl:String,
  @(ApiModelProperty @field)(description = "Url to where a cover photo can be found") coverPhotoUrl:Option[String],
  @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes") duration:Int,
  @(ApiModelProperty @field)(description = "The publishing status of the learningpath.", allowableValues = "PUBLISHED,PRIVATE,NOT_LISTED") status:String,
  @(ApiModelProperty @field)(description = "The date when this learningpath was last updated.") lastUpdated:Date,
  @(ApiModelProperty @field)(description = "The author of this learningpath") author:Author
)

@ApiModel(description = "Information about a learningstep")
case class LearningStep(
  @(ApiModelProperty @field)(description = "The id of the learningstep") id:Long,
  @(ApiModelProperty @field)(description = "The sequence number for the step. The first step has seqNo 0.") seqNo:Int,
  @(ApiModelProperty @field)(description = "The titles of the learningstep") title:List[Title],
  @(ApiModelProperty @field)(description = "The descriptions of the learningstep") description:List[Description],
  @(ApiModelProperty @field)(description = "The embed urls for the learningstep") embedUrl:List[EmbedUrl],
  @(ApiModelProperty @field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`:String,
  @(ApiModelProperty @field)(description = "The license for this step.") license:Option[String],
  @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningstep can be found") metaUrl:String
)

@ApiModel(description = "Information about a new learningstep")
case class NewLearningStep(
  @(ApiModelProperty @field)(description = "The titles of the learningstep") title:List[Title],
  @(ApiModelProperty @field)(description = "The descriptions of the learningstep") description:List[Description],
  @(ApiModelProperty @field)(description = "The embed urls for the learningstep") embedUrl:List[EmbedUrl],
  @(ApiModelProperty @field)(description = "The type of the step", allowableValues = "INTRODUCTION,TEXT,QUIZ,TASK,MULTIMEDIA,SUMMARY,TEST") `type`:String,
  @(ApiModelProperty @field)(description = "The license for this step. Must be plain text") license:Option[String]
) {
  def validate(): NewLearningStep = {
    val validationResult = ComponentRegistry.newLearningStepValidator.validate(this)
    validationResult.isEmpty match {
      case true => this
      case false => throw new ValidationException(errors = validationResult)
    }
  }
}

@ApiModel(description = "Representation of a title")
case class Title(
  @(ApiModelProperty @field)(description = "The title of the content. Must be plain text") title:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in title") language:Option[String]
)

@ApiModel(description = "Representation of an embeddable url")
case class EmbedUrl(
  @(ApiModelProperty @field)(description = "The url") url:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in title") language:Option[String]
)

@ApiModel(description = "The description of the learningpath")
case class Description(
  @(ApiModelProperty @field)(description = "The learningpath description. Basic HTML allowed") description:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in description") language:Option[String]
)

@ApiModel(description = "Information about an author")
case class Author(
  @(ApiModelProperty @field)(description = "The description of the author. Eg. Photographer or Supplier") `type`:String,
  @(ApiModelProperty @field)(description = "The name of the of the author") name:String
)

case class LearningPathTag(
  @(ApiModelProperty @field)(description = "The searchable tag. Must be plain text") tag:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in tag") language:Option[String]
)

@ApiModel(description = "A message describing a validation error on a specific field")
case class ValidationMessage(
  @(ApiModelProperty @field)(description = "The field the error occured in") field: String,
  @(ApiModelProperty @field)(description = "The validation message") message: String)

@ApiModel(description = "Information about validation errors")
case class ValidationError(
  @(ApiModelProperty @field)(description = "Code stating the type of error") code:String = Error.VALIDATION,
  @(ApiModelProperty @field)(description = "Description of the error") description:String = Error.VALIDATION_DESCRIPTION,
  @(ApiModelProperty @field)(description = "List of validation messages") messages: List[ValidationMessage],
  @(ApiModelProperty @field)(description = "When the error occured") occuredAt:Date = new Date())

@ApiModel(description = "Information about an error")
case class Error(
  @(ApiModelProperty @field)(description = "Code stating the type of error") code:String = Error.GENERIC,
  @(ApiModelProperty @field)(description = "Description of the error") description:String = Error.GENERIC_DESCRIPTION,
  @(ApiModelProperty @field)(description = "When the error occured") occuredAt:Date = new Date())

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val INDEX_MISSING = "INDEX_MISSING"
  val HEADER_MISSING = "HEADER_MISSING"
  val VALIDATION = "VALIDATION"
  val ACCESS_DENIED = "ACCESS_DENIED"

  val GENERIC_DESCRIPTION = s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${LearningpathApiProperties.ContactEmail} if the error persists."
  val VALIDATION_DESCRIPTION = "Validation Error"
}


case class ResponseMessageWithModel(code: Int, message: String, responseModel: String) extends ResponseMessage[String]