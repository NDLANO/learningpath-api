/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
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
    implicit val formats: Formats = ConfigMeta.formats
    implicit val configValueParameterBinderFactory: ParameterBinderFactory[ConfigMeta] =
      ParameterBinderFactory[ConfigMeta] { value => (stmt, idx) =>
        {
          val dataObject = new PGobject()
          dataObject.setType("jsonb")
          dataObject.setValue(write(value))
          stmt.setObject(idx, dataObject)
        }
      }

    def configCount(implicit session: DBSession = ReadOnlyAutoSession): Int = {
      val c = ConfigMeta.syntax("c")
      withSQL {
        select(count(c.column("configkey"))).from(ConfigMeta as c)
      }.map(_.int(1)).single().getOrElse(0)
    }

    def updateConfigParam(config: ConfigMeta)(implicit session: DBSession = AutoSession): Try[ConfigMeta] = {
      val updatedCount = withSQL {
        update(ConfigMeta)
          .set(ConfigMeta.column.column("value") -> config)
          .where
          .eq(ConfigMeta.column.column("configkey"), config.key.toString)
      }.update()

      if (updatedCount != 1) {
        logger.info(s"No existing value for ${config.key}, inserting the value.")
        withSQL {
          insertInto(ConfigMeta).namedValues(
            ConfigMeta.column.c("configkey") -> config.key.toString,
            ConfigMeta.column.c("value") -> config,
          )
        }.update()
        Success(config)
      } else {
        logger.info(s"Value for ${config.key} updated.")
        Success(config)
      }
    }

    def getConfigWithKey(key: ConfigKey.Value)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[ConfigMeta] = {
      val c = ConfigMeta.syntax("c")
      sql"""
           select ${c.result.*}
           from ${ConfigMeta.as(c)}
           where configkey = ${key.toString};
        """
        .map(ConfigMeta(c))
        .single()
    }
  }
}
