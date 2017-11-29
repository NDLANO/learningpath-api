/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import io.searchbox.client.JestResult
import no.ndla.learningpathapi.model.api.ValidationMessage

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage]) extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)
class OptimisticLockException(message: String) extends RuntimeException(message)
class ImportException(message: String) extends  RuntimeException(message)
class NdlaSearchException(jestResponse: JestResult) extends RuntimeException(jestResponse.getErrorMessage) {
  def getResponse: JestResult = jestResponse
}
class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
case class LanguageNotSupportedException(message: String) extends RuntimeException(message)