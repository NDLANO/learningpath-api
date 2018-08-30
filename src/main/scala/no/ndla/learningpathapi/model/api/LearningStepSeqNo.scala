/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the sequence number for a step")
case class LearningStepSeqNo(
    @(ApiModelProperty @field)(
      description = "The sequence number for the learningstep") seqNo: Int)
