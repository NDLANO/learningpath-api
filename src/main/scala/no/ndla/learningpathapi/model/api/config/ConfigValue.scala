/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api.config

import java.util.Date

import no.ndla.learningpathapi.model.domain.config.ConfigKey
import org.scalatra.swagger.annotations.ApiModelProperty
import org.scalatra.swagger.runtime.annotations.ApiModel

import scala.annotation.meta.field

@ApiModel(description = "Describes configuration value.")
case class ConfigValue(
    @(ApiModelProperty @field)(description = "Configuration key") key: String,
    @(ApiModelProperty @field)(description = "Configuration value.") value: String,
    @(ApiModelProperty @field)(description = "Date of when configuration was last updated") updatedAt: Date,
    @(ApiModelProperty @field)(description = "UserId of who last updated the configuration parameter.") updatedBy: String
)
