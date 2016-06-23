package no.ndla.learningpathapi.model.domain

case class Description(description:String, language:Option[String]) extends LanguageField {
  override def value = description
}