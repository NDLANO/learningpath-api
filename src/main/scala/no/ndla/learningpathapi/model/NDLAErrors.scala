package no.ndla.learningpathapi.model

import no.ndla.learningpathapi.ValidationMessage

class ValidationException(message: String = "Validation Error", val errors: List[ValidationMessage]) extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)