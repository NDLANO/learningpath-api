/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Representation of an embeddable url")
case class EmbedUrlV2(
  @(ApiModelProperty @field)(description = "The url") url: String,
  @(ApiModelProperty @field)(description = "Type of embed content", allowableValues = "oembed,iframe,lti") embedType: String
)
