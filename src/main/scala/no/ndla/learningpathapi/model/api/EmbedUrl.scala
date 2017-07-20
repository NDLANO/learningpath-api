/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Representation of an embeddable url")
case class EmbedUrl(@(ApiModelProperty@field)(description = "The url") url: String,
                    @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in the embeddable content") language: Option[String],
                    @(ApiModelProperty@field)(description = "Type of embed content", allowableValues = "oembed,lti") embedType: String)

@ApiModel(description = "Representation of an embeddable url")
case class EmbedUrlV2(@(ApiModelProperty@field)(description = "The url") url: String,
                      @(ApiModelProperty@field)(description = "Type of embed content", allowableValues = "oembed,lti") embedType: String)
