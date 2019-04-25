/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api.config

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Info for updating a configuration parameter")
case class UpdateConfigValue(
    @(ApiModelProperty @field)(description = "Value to set configuration param to.") value: Boolean
)
