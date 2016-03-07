package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{ValidationMessage, TestEnvironment, EmbedUrl, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._

class EmbedUrlValidatorTest extends UnitSuite with TestEnvironment{

  var validator: EmbedUrlValidator = _
  override def beforeEach() = {
    validator = new EmbedUrlValidator
  }

  val DefaultEmbedUrl = EmbedUrl("http://www.ndla.no/123/oembed", Some("nb"))

  test("That url is validated") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("embedUrl.url", "Invalid")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationMessages = validator.validate(List(DefaultEmbedUrl))
    validationMessages.size should be (1)
    validationMessages.head.field should equal ("embedUrl.url")
    validationMessages.head.message should equal ("Invalid")
  }

  test("That language is validated") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("embedUrl.language", "Invalid")))

    val validationMessages = validator.validate(List(DefaultEmbedUrl))
    validationMessages.size should be (1)
    validationMessages.head.field should equal ("embedUrl.language")
    validationMessages.head.message should equal ("Invalid")
  }

  test("That both url and language are validated") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("embedUrl.url", "Invalid url")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(Some(ValidationMessage("embedUrl.language", "Invalid language")))

    val validationMessages = validator.validate(List(DefaultEmbedUrl))
    validationMessages.size should be (2)
    validationMessages.head.field should equal ("embedUrl.url")
    validationMessages.head.message should equal ("Invalid url")
    validationMessages.last.field should equal ("embedUrl.language")
    validationMessages.last.message should equal ("Invalid language")
  }

  test("That valid embedUrl does not give error") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)
    validator.validate(List(DefaultEmbedUrl)) should equal (List())
  }

  test("That all embedUrls are validated") {
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("embedUrl.url", "Invalid url")))
    when(languageValidator.validate(any[String], any[Option[String]])).thenReturn(None)

    val validationMessages = validator.validate(List(DefaultEmbedUrl, DefaultEmbedUrl))
    validationMessages.size should be (2)
    validationMessages.head.field should equal ("embedUrl.url")
    validationMessages.head.message should equal ("Invalid url")
    validationMessages.last.field should equal ("embedUrl.url")
    validationMessages.last.message should equal ("Invalid url")
  }
}
