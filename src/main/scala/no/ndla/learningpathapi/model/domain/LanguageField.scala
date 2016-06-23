package no.ndla.learningpathapi.model.domain

trait LanguageField {
  def value: String
  def language: Option[String]
}