package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.model.api.ValidationMessage

class ValidationException(message: String = "Validation Error", val errors: List[ValidationMessage]) extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)
class HttpRequestException(message: String) extends RuntimeException(message)