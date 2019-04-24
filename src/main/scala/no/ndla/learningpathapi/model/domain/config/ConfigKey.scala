/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

object ConfigKey extends Enumeration {
  val IsExamPeriod: ConfigKey.Value = Value("IS_EXAM_PERIOD")
  def valueOf(s: String): Option[ConfigKey.Value] = ConfigKey.values.find(_.toString == s)
}
