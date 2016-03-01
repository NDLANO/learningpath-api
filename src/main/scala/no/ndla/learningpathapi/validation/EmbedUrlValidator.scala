package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{ValidationMessage, EmbedUrl}

object EmbedUrlValidator {
  val URL_EMTPY = "Required value url is empty."

  def validate(embedUrls: List[EmbedUrl]): List[ValidationMessage] = {
    embedUrls.flatMap(embedUrl => validate(embedUrl))
  }

  def validate(embedUrl: EmbedUrl): List[ValidationMessage] = {
    val urlResult = if(embedUrl.url.isEmpty) Some(ValidationMessage("embedUrl.url", URL_EMTPY)) else None
    List(urlResult, embedUrl.language.flatMap(embedUrl => LanguageValidator.validate("embedUrl.language", embedUrl))).flatMap(nonEmpty => nonEmpty)
  }
}
