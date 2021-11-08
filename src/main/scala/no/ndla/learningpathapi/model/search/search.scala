/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model

import no.ndla.learningpathapi.model.domain.WithLanguage

package object search {

  case class LanguageValue[T](lang: String, value: T) extends WithLanguage {
    override def language: String = lang
  }
}
