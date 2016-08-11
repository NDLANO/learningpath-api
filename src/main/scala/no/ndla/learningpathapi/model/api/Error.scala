/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about an error")
case class Error(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String = Error.GENERIC,
                 @(ApiModelProperty@field)(description = "Description of the error") description: String = Error.GENERIC_DESCRIPTION,
                 @(ApiModelProperty@field)(description = "When the error occured") occuredAt: Date = new Date())

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val INDEX_MISSING = "INDEX_MISSING"
  val HEADER_MISSING = "HEADER_MISSING"
  val VALIDATION = "VALIDATION"
  val ACCESS_DENIED = "ACCESS_DENIED"
  val REMOTE_ERROR = "REMOTE_ERROR"
  val RESOURCE_OUTDATED = "RESOURCE_OUTDATED"

  val GENERIC_DESCRIPTION = s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${LearningpathApiProperties.ContactEmail} if the error persists."
  val VALIDATION_DESCRIPTION = "Validation Error"
  val RESOURCE_OUTDATED_DESCRIPTION = "The resource is outdated. Please try fetching before submitting again."
}