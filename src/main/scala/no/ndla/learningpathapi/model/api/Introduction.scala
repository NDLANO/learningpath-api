package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "The introduction of the learningpath")
case class Introduction(@(ApiModelProperty@field)(description = "The introduction to the learningpath. Basic HTML allowed") introduction: String,
                        @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in introduction") language: Option[String])
