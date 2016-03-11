package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.LearningpathApiProperties._
import no.ndla.learningpathapi.model.api.ValidationMessage
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist


trait TextValidatorComponent {
  val basicHtmlTextValidator: TextValidator
  val noHtmlTextValidator: TextValidator

  class TextValidator(allowHtml: Boolean) {
    val IllegalContentInBasicText = s"The content contains illegal html-characters. Allowed characters are ${BasicHtmlTags.mkString(", ")}"
    val IllegalContentInPlainText = "The content contains illegal html-characters. No HTML is allowed."
    val FieldEmpty = "Required field is empty."

    def validate(fieldPath: String, text: String): Option[ValidationMessage] = {
      allowHtml match {
        case true => validateOnlyBasicHtmlTags(fieldPath, text)
        case false => validateNoHtmlTags(fieldPath, text)
      }
    }

    private def validateOnlyBasicHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      text.isEmpty match {
        case true => Some(ValidationMessage(fieldPath, FieldEmpty))
        case false => {
          Jsoup.isValid(text, new Whitelist().addTags(BasicHtmlTags:_*)) match {
            case true => None
            case false => Some(ValidationMessage(fieldPath, IllegalContentInBasicText))
          }
        }
      }
    }

    private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      Jsoup.isValid(text, Whitelist.none()) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, IllegalContentInPlainText))
      }
    }
  }

}
