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

@ApiModel(description = "Status information about a learningpath")
case class LearningStepStatus(@(ApiModelProperty @field)(description = "The status of the learningstep",
                                                         allowableValues = "ACTIVE,DELETED") status: String)
