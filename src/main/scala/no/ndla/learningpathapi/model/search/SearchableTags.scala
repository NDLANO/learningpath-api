/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.search

case class SearchableTags (nb: Option[Seq[String]],
                           nn: Option[Seq[String]],
                           en: Option[Seq[String]],
                           fr: Option[Seq[String]],
                           de: Option[Seq[String]],
                           es: Option[Seq[String]],
                           se: Option[Seq[String]],
                           zh: Option[Seq[String]],
                           unknown: Option[Seq[String]])
