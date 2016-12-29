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