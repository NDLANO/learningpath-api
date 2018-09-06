/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

case class Title(title: String, language: String) extends LanguageField[String] {
  override def value: String = title
}
