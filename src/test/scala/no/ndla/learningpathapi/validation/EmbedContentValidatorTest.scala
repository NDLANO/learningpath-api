package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{ValidationMessage, TestEnvironment, EmbedContent, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._

class EmbedContentValidatorTest extends UnitSuite with TestEnvironment{

  var validator: EmbedContentValidator = _
  override def beforeEach() = {
    validator = new EmbedContentValidator
  }

  val DefaultEmbedContent = EmbedContent("http://www.ndla.no/123/oembed", "<iframe src='http://ndla.no/en/node/137355/oembed' allowfullscreen></iframe>", Some("nb"))

  test("That url is validated") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("embedContent.url", "Invalid")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationMessages = validator.validate(List(DefaultEmbedContent))
    validationMessages.size should be (1)
    validationMessages.head.field should equal ("embedContent.url")
    validationMessages.head.message should equal ("Invalid")
  }

  test("That language is validated") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("embedContent.language", "Invalid")))

    val validationMessages = validator.validate(List(DefaultEmbedContent))
    validationMessages.size should be (1)
    validationMessages.head.field should equal ("embedContent.language")
    validationMessages.head.message should equal ("Invalid")
  }

  test("That both url and language are validated") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("embedContent.url", "Invalid url")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("embedContent.language", "Invalid language")))

    val validationMessages = validator.validate(List(DefaultEmbedContent))
    validationMessages.size should be (2)
    validationMessages.head.field should equal ("embedContent.url")
    validationMessages.head.message should equal ("Invalid url")
    validationMessages.last.field should equal ("embedContent.language")
    validationMessages.last.message should equal ("Invalid language")
  }

  test("That valid embedUrl does not give error") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)
    validator.validate(List(DefaultEmbedContent)) should equal (List())
  }

  test("That all embedUrls are validated") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("embedContent.url", "Invalid url")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationMessages = validator.validate(List(DefaultEmbedContent, DefaultEmbedContent))
    validationMessages.size should be (2)
    validationMessages.head.field should equal ("embedContent.url")
    validationMessages.head.message should equal ("Invalid url")
    validationMessages.last.field should equal ("embedContent.url")
    validationMessages.last.message should equal ("Invalid url")
  }
}
