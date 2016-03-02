package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.ValidationMessage
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist


object TextValidator {

  val BasicHtmlTags = List("b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em",
    "i", "li", "ol", "p", "pre", "q", "small", "strike", "strong",
    "sub", "sup", "u", "ul")

  val IllegalContentInBasicText = s"The content contains illegal html-characters. Allowed characters are ${BasicHtmlTags.mkString(", ")}"
  val IllegalContentInPlainText = "The content contains illegal html-characters. No HTML is allowed."

  def validateOnlyBasicHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
    Jsoup.isValid(text, new Whitelist().addTags(BasicHtmlTags:_*)) match {
      case true => None
      case false => Some(ValidationMessage(fieldPath, IllegalContentInBasicText))
    }
  }
  
  def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
    Jsoup.isValid(text, Whitelist.none()) match {
      case true => None
      case false => Some(ValidationMessage(fieldPath, IllegalContentInPlainText))
    }
  }
}
