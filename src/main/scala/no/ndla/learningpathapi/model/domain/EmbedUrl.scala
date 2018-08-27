/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.model.api.ValidationMessage

case class EmbedUrl(url: String, language: String, embedType: EmbedType.Value)
    extends LanguageField[String] {
  override def value: String = url
}

object EmbedType extends Enumeration {

  val OEmbed = Value("oembed")
  val LTI = Value("lti")

  def valueOf(s: String): Option[EmbedType.Value] = {
    EmbedType.values.find(_.toString == s)
  }

  def valueOfOrError(embedType: String): EmbedType.Value = {
    valueOf(embedType) match {
      case Some(s) => s
      case None =>
        throw new ValidationException(
          errors = List(
            ValidationMessage("embedType",
                              s"'$embedType' is not a valid embed type.")))
    }
  }

  def valueOfOrDefault(s: String): EmbedType.Value = {
    valueOf(s).getOrElse(EmbedType.OEmbed)
  }
}
