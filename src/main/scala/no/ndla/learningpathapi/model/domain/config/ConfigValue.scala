/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import org.joda.time.DateTime
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc.{WrappedResultSet, _}

case class ConfigValue(
    key: ConfigKey.Value,
    value: String,
    updatedAt: Date,
    updatedBy: String
)

object ConfigValue extends SQLSyntaxSupport[ConfigValue] {
  implicit val formats: Formats = org.json4s.DefaultFormats +
    new EnumNameSerializer(ConfigKey) ++
    org.json4s.ext.JodaTimeSerializers.all

  override val tableName = "configtable"
  override val schemaName = Some(LearningpathApiProperties.MetaSchema)

  def apply(c: SyntaxProvider[ConfigValue])(rs: WrappedResultSet): ConfigValue = apply(c.resultName)(rs)

  def apply(c: ResultName[ConfigValue])(rs: WrappedResultSet): ConfigValue = {
    val meta = read[ConfigValue](rs.string(c.column("value")))
    meta
  }
}
