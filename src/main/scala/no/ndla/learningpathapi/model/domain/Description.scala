/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

case class Description(description:String, language:Option[String]) extends LanguageField[String] {
  override def value: String = description
}