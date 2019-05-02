/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.api.ValidationMessage
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc.{WrappedResultSet, _}
import no.ndla.learningpathapi.model.domain.ValidationException

import scala.util.{Failure, Success, Try}

case class ConfigMeta(
    key: ConfigKey.Value,
    value: String,
    updatedAt: Date,
    updatedBy: String
) {

  def validate: Try[ConfigMeta] = {
    key match {
      case ConfigKey.IsWriteRestricted =>
        Try(value.toBoolean) match {
          case Success(_) => Success(this)
          case Failure(_) =>
            val validationMessage = ValidationMessage(
              "value",
              s"Value of '${ConfigKey.IsWriteRestricted.toString}' must be a boolean string ('true' or 'false')")
            Failure(new ValidationException(s"Invalid config value specified.", Seq(validationMessage)))
        }
      // Add case here for validation for new ConfigKeys
    }
  }
}

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
