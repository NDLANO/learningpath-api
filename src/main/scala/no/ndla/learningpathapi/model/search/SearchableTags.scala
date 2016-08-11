/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.search

case class SearchableTags (nb: Seq[String],
                           nn: Seq[String],
                           en: Seq[String],
                           fr: Seq[String],
                           de: Seq[String],
                           es: Seq[String],
                           se: Seq[String],
                           zh: Seq[String],
                           unknown: Seq[String])
