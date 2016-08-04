package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Representation of an embeddable url")
case class EmbedUrl(@(ApiModelProperty@field)(description = "The url") url: String,
                        @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in the embeddable content") language: Option[String])