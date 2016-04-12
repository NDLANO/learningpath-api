package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Representation of a title")
case class Title(@(ApiModelProperty@field)(description = "The title of the content. Must be plain text") title: String,
                 @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: Option[String])