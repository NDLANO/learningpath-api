/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

object ImportStatus extends Enumeration {
  val OK = Value("OK")
  val ERROR = Value("ERROR")
}
case class ImportReport(nid: String, status: ImportStatus.Value, messages: Seq[String], id: Option[Long]) extends RuntimeException(s"$nid - $status: ${messages.mkString(",")}")
