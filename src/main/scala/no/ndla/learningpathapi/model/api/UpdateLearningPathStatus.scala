/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Status information about a learningpath")
case class UpdateLearningPathStatus(
    @(ApiModelProperty @field)(description = "The publishing status of the learningpath",
                               allowableValues = "PUBLISHED,PRIVATE,DELETED,UNLISTED,SUBMITTED") status: String,
    @(ApiModelProperty @field)(description =
      "Message that admins can place on a LearningPath for notifying a owner of issues with the LearningPath") message: Option[
      String])
