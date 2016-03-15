package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "The description of the learningpath")
case class Description(@(ApiModelProperty@field)(description = "The learningpath description. Basic HTML allowed") description: String,
                       @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in description") language: Option[String])
