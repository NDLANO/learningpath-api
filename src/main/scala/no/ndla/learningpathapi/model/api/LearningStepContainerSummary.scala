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

@ApiModel(description = "Summary of meta information for a learningstep including language and supported languages")
case class LearningStepContainerSummary(
    @(ApiModelProperty @field)(description = "The chosen search language") language: String,
    @(ApiModelProperty @field)(description = "The chosen search language") learningsteps: Seq[LearningStepSummaryV2],
    @(ApiModelProperty @field)(description = "The chosen search language") supportedLanguages: Seq[String])
