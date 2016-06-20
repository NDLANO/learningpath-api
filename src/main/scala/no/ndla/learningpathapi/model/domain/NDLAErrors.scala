package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.model.api.ValidationMessage

import scalaj.http.HttpResponse

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage]) extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)
class OptimisticLockException(message: String) extends RuntimeException(message)
class HttpRequestException(message: String, httpResponse: Option[HttpResponse[String]] = None) extends RuntimeException(message) {
  def is404:Boolean = {
    httpResponse.exists(_.isCodeInRange(404, 404))
  }
}
