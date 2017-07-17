package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class LearningPathTagsSummary(@(ApiModelProperty@field)(description = "The chosen language. Default is 'nb'") language: String,
                                   @(ApiModelProperty@field)(description = "The supported languages for these tags") supportedLanguages: Seq[String],
                                   @(ApiModelProperty@field)(description = "The searchable tags. Must be plain text") tags: Seq[String]
                                  )

