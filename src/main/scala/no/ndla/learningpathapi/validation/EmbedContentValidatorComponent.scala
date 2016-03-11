package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.{EmbedContent, ValidationMessage}

trait EmbedContentValidatorComponent {
  this: TextValidatorComponent with LanguageValidatorComponent =>
  val embedContentValidator: EmbedContentValidator

  class EmbedContentValidator {
    def validate(embedContents: List[EmbedContent]): List[ValidationMessage] = {
      embedContents.flatMap(embedContent => {
        noHtmlTextValidator.validate("embedContent.url", embedContent.url).toList :::
        languageValidator.validate("embedContent.language", embedContent.language).toList
      })
    }
  }
}
