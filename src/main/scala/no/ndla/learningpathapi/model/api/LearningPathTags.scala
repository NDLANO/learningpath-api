package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class LearningPathTags(@(ApiModelProperty@field)(description = "The searchable tags. Must be plain text") tags: Seq[String],
                            @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in tag") language: Option[String])
