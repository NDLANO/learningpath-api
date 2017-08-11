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

@ApiModel(description = "Representation of a title")
case class Title(@(ApiModelProperty@field)(description = "The title of the content. Must be plain text") title: String,
                 @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: String) extends LanguageField[String] { override def value: String = title }
