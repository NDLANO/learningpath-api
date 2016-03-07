package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{ValidationMessage, EmbedUrl}

trait EmbedUrlValidatorComponent {
  this: TextValidatorComponent with LanguageValidatorComponent =>
  val embedUrlValidator: EmbedUrlValidator

  class EmbedUrlValidator {
    def validate(embedUrls: List[EmbedUrl]): List[ValidationMessage] = {
      embedUrls.flatMap(embedUrl => {
        noHtmlTextValidator.validate("embedUrl.url", embedUrl.url).toList :::
        languageValidator.validate("embedUrl.language", embedUrl.language).toList
      })
    }
  }
}
