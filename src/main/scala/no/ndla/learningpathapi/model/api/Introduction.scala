/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import no.ndla.learningpathapi.model.domain.LanguageField
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "The introduction of the learningpath")
case class Introduction(@(ApiModelProperty@field)(description = "The introduction to the learningpath. Basic HTML allowed") introduction: String,
                        @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in introduction") language: String) extends LanguageField[String] { override def value: String = introduction }
