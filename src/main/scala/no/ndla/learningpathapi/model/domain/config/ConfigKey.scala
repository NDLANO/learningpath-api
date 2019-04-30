/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

object ConfigKey extends Enumeration {
  val IsWriteRestricted: ConfigKey.Value = Value("IS_WRITE_RESTRICTED")
  def valueOf(s: String): Option[ConfigKey.Value] = ConfigKey.values.find(_.toString == s)
}
