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

@ApiModel(description = "Status information about a learningpath")
case class LearningPathStatus(
    @(ApiModelProperty @field)(description = "The publishing status of the learningpath",
                               allowableValues = "PUBLISHED,PRIVATE,DELETED,UNLISTED,SUBMITTED") status: String)
