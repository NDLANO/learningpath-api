/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.model.api.ValidationMessage

case class EmbedUrl(url:String, language:Option[String], embedType: String) extends LanguageField {
  override def value = url
}

object EmbedType extends Enumeration {
  val oembed, lti = Value

  def valueOf(s:String): Option[EmbedType.Value] = {
    EmbedType.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrError(embedType: String): EmbedType.Value = {
    valueOf(embedType) match {
      case Some(embedType) => embedType
      case None => throw new ValidationException(errors = List(ValidationMessage("embedtype", s"'$embedType' is not a valid embedtype.")))
    }
  }

  def valueOfOrDefault(s:String): EmbedType.Value = {
    valueOf(s).getOrElse(EmbedType.oembed)
  }
}