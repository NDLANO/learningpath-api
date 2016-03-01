package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{EmbedUrl, UnitSuite}

class EmbedUrlValidatorTest extends UnitSuite {

  val DefaultEmbedUrl = EmbedUrl("http://www.ndla.no/123/oembed", Some("nb"))

  test("That an empty string as embed url gives an error") {
    val validationMessages = EmbedUrlValidator.validate(List(DefaultEmbedUrl.copy(url = "")))
    validationMessages.size should be (1)
    validationMessages.head.field should equal ("embedUrl.url")
    validationMessages.head.message should equal ("Required value url is empty.")
  }

  test("That invalid language gives an error") {
    val validationMessages = EmbedUrlValidator.validate(List(DefaultEmbedUrl.copy(language = Some("Unsupported"))))
    validationMessages.size should be (1)
    validationMessages.head.field should equal ("embedUrl.language")
    validationMessages.head.message should equal ("Language 'Unsupported' is not a supported value.")
  }

  test("That both invalid url and invalid language gives two errors") {
    val validationMessages = EmbedUrlValidator.validate(List(DefaultEmbedUrl.copy(url = "", language = Some("Unsupported"))))
    validationMessages.size should be (2)
    validationMessages.head.field should equal ("embedUrl.url")
    validationMessages.head.message should equal ("Required value url is empty.")
    validationMessages.last.field should equal ("embedUrl.language")
    validationMessages.last.message should equal ("Language 'Unsupported' is not a supported value.")
  }

  test("That two embedUrls with invalid urls gives two errors") {
    val validationMessages = EmbedUrlValidator.validate(List(
        DefaultEmbedUrl.copy(url = ""),
        DefaultEmbedUrl.copy(url = "")))

    validationMessages.size should be (2)
    validationMessages.head.field should equal ("embedUrl.url")
    validationMessages.head.message should equal ("Required value url is empty.")
    validationMessages.last.field should equal ("embedUrl.url")
    validationMessages.last.message should equal ("Required value url is empty.")
  }

  test("That a valid embedUrl gives no error") {
    EmbedUrlValidator.validate(List(DefaultEmbedUrl)) should equal(List())
  }
}
