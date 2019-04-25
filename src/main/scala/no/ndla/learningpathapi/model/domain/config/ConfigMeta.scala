/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc.{WrappedResultSet, _}

case class ConfigMeta(
    key: ConfigKey.Value,
    value: Boolean,
    updatedAt: Date,
    updatedBy: String
)

object ConfigMeta extends SQLSyntaxSupport[ConfigMeta] {
  implicit val formats: Formats = org.json4s.DefaultFormats +
    new EnumNameSerializer(ConfigKey) ++
    org.json4s.ext.JodaTimeSerializers.all

  override val tableName = "configtable"
  override val schemaName = Some(LearningpathApiProperties.MetaSchema)

  def apply(c: SyntaxProvider[ConfigMeta])(rs: WrappedResultSet): ConfigMeta = apply(c.resultName)(rs)

  def apply(c: ResultName[ConfigMeta])(rs: WrappedResultSet): ConfigMeta = {
    val meta = read[ConfigMeta](rs.string(c.column("value")))
    meta
  }
}
