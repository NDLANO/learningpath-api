/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigValue}
import org.json4s.Formats
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._
import sqls.count

import scala.util.{Success, Try}

trait ConfigRepository {
  this: DataSource =>
  val configRepository: ConfigRepository

  class ConfigRepository extends LazyLogging {
    implicit val formats: Formats = ConfigValue.formats
    implicit val configValueParameterBinderFactory: ParameterBinderFactory[ConfigValue] =
      ParameterBinderFactory[ConfigValue] { value => (stmt, idx) =>
        {
          val dataObject = new PGobject()
          dataObject.setType("jsonb")
          dataObject.setValue(write(value))
          stmt.setObject(idx, dataObject)
        }
      }

    def configCount(implicit session: DBSession = ReadOnlyAutoSession): Int = {
      val c = ConfigValue.syntax("c")
      withSQL {
        select(count(c.column("configkey"))).from(ConfigValue as c)
      }.map(_.int(1)).single.apply.getOrElse(0)
    }

    def updateConfigParam(config: ConfigValue)(implicit session: DBSession = AutoSession): Try[_] = {
      val updatedCount = withSQL {
        update(ConfigValue)
          .set(ConfigValue.column.column("value") -> config)
          .where
          .eq(ConfigValue.column.column("configkey"), config.key.toString)
      }.update.apply()

      if (updatedCount != 1) {
        logger.info(s"No existing value for ${config.key}, inserting the value.")
        withSQL {
          insertInto(ConfigValue).namedValues(
            ConfigValue.column.c("configkey") -> config.key.toString,
            ConfigValue.column.c("value") -> config,
          )
        }.update.apply()
        Success(config)
      } else {
        logger.info(s"Value for ${config.key} updated.")
        Success(config)
      }
    }

    def getConfigWithKey(key: ConfigKey.Value)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[ConfigValue] = {
      val c = ConfigValue.syntax("c")
      sql"""
           select ${c.result.*}
           from ${ConfigValue.as(c)}
           where configkey = ${key.toString};
        """
        .map(ConfigValue(c))
        .single()
        .apply()
    }
  }
}
