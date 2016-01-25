package no.ndla.learningpathapi.model

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a learningpath")
case class Learningpath(
  @(ApiModelProperty @field)(description = "The unique id of the learningpath") id:String,
  @(ApiModelProperty @field)(description = "The titles of the learningpath") title:List[Title],
  @(ApiModelProperty @field)(description = "The descriptions of the learningpath") description:List[Description],
  @(ApiModelProperty @field)(description = "The learningsteps for this learningpath") learningsteps:List[Learningstep],
  @(ApiModelProperty @field)(description = "The full url to where the learningsteps can be found") learningstepUrl:String,
  @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningpath can be found") metaUrl:String,
  @(ApiModelProperty @field)(description = "Url to where a cover photo can be found") coverPhotoUrl:Option[String],
  @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes") duration:Int,
  @(ApiModelProperty @field)(description = "The publishing status of the learningpath. Either 'PUBLISHED' or 'PRIVATE'") status:String,
  @(ApiModelProperty @field)(description = "The author of this learningpath") author:Author
)

@ApiModel(description = "Status information about a learningpath")
case class LearningpathStatus(
  @(ApiModelProperty @field)(description = "The publishing status of the learningpath. Either 'PUBLISHED' or 'PRIVATE'") status:String
)

@ApiModel(description = "Summary of meta information for a learningpath")
case class LearningpathSummary(
  @(ApiModelProperty @field)(description = "The unique id of the learningpath") id:String,
  @(ApiModelProperty @field)(description = "The titles of the learningpath") title:List[Title],
  @(ApiModelProperty @field)(description = "The descriptions of the learningpath") description:List[Description],
  @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningpath can be found") metaUrl:String,
  @(ApiModelProperty @field)(description = "Url to where a cover photo can be found") coverPhotoUrl:Option[String],
  @(ApiModelProperty @field)(description = "The duration of the learningpath in minutes") duration:Int,
  @(ApiModelProperty @field)(description = "The publishing status of the learningpath. Either 'PUBLISHED' or 'PRIVATE'") status:String,
  @(ApiModelProperty @field)(description = "The author of this learningpath") author:Author
)

@ApiModel(description = "Summary of meta information for a learningpath")
case class Learningstep(
  @(ApiModelProperty @field)(description = "The id of the learningstep") id:Int,
  @(ApiModelProperty @field)(description = "The sequence number for the step") seqNo:Int,
  @(ApiModelProperty @field)(description = "The titles of the learningstep") title:List[Title],
  @(ApiModelProperty @field)(description = "The embed urls for the learningstep") embedUrl:List[EmbedUrl],
  @(ApiModelProperty @field)(description = "The type of the step. One of YOUTUBE|TEXT|ETC..") `type`:String,
  @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the learningstep can be found") metaUrl:String
)

@ApiModel(description = "Representation of a title")
case class Title(
  @(ApiModelProperty @field)(description = "The title of the content") title:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in title") language:Option[String]
)

@ApiModel(description = "Representation of an embeddable url")
case class EmbedUrl(
  @(ApiModelProperty @field)(description = "The url") url:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in title") language:Option[String]
)

@ApiModel(description = "The description of the learningpath")
case class Description(
  @(ApiModelProperty @field)(description = "The learningpath description") description:String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in description") language:Option[String]
)

@ApiModel(description = "Information about an author")
case class Author(
  @(ApiModelProperty @field)(description = "The description of the author. Eg. Photographer or Supplier") `type`:String,
  @(ApiModelProperty @field)(description = "The name of the of the author") name:String
)

