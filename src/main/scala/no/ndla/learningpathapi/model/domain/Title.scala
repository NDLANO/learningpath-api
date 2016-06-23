package no.ndla.learningpathapi.model.domain

case class Title(title:String, language:Option[String]) extends LanguageField {
  override def value = title
}