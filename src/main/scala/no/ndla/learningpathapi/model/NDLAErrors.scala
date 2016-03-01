package no.ndla.learningpathapi.model

import java.text.SimpleDateFormat
import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties


object Error extends Enumeration{
  val GENERIC, NOT_FOUND, INDEX_MISSING, HEADER_MISSING, VALIDATION, ACCESS_DENIED = Value
  val GenericError = Error(GENERIC, s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${LearningpathApiProperties.ContactEmail} if the error persists.")
}

case class Error(code:Error.Value, description:String, occuredAt:String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))

case class ValidationMessage(field: String, message: String)

case class ValidationError(code:Error.Value = Error.VALIDATION,
                           description:String = "Validation Error",
                           messages: List[ValidationMessage],
                           occuredAt:String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))

class HeaderMissingException(message: String) extends RuntimeException(message)
class ValidationException(message: String = "Validation Error", val errors: List[ValidationMessage]) extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)