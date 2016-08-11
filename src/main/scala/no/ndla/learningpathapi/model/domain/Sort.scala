/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

object Sort extends Enumeration {
  val ByRelevanceDesc = Value("-relevance")
  val ByRelevanceAsc = Value("relevance")
  val ByLastUpdatedDesc = Value("-lastUpdated")
  val ByLastUpdatedAsc = Value("lastUpdated")
  val ByDurationDesc = Value("-duration")
  val ByDurationAsc = Value("duration")
  val ByTitleDesc = Value("-title")
  val ByTitleAsc = Value("title")

  def valueOf(s:String): Option[Sort.Value] = {
    Sort.values.find(_.toString == s)
  }

  def valueOf(s:Option[String]): Option[Sort.Value] = {
    s match {
      case None => None
      case Some(s) => valueOf(s)
    }
  }
}
