/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api
import java.util.Date
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Administrator message left on learningpaths")
case class Message(
    @(ApiModelProperty @field)(description = "Message left on a learningpath by administrator") message: String,
    @(ApiModelProperty @field)(description = "When the message was left") date: Date)
