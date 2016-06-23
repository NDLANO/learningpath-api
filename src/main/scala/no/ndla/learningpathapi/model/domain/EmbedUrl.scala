package no.ndla.learningpathapi.model.domain

case class EmbedUrl(url:String, language:Option[String]) extends LanguageField {
  override def value = url
}