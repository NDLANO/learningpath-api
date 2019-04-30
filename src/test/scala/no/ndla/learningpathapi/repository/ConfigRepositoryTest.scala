/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import java.net.Socket
import java.util.Date

import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
import no.ndla.learningpathapi.{DBMigrator, IntegrationSuite, LearningpathApiProperties, TestEnvironment}
import no.ndla.tag.IntegrationTest
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}

import scala.util.{Success, Try}

@IntegrationTest
class ConfigRepositoryTest extends IntegrationSuite with TestEnvironment {
  var repository: ConfigRepository = _

  def serverIsListenning: Boolean = {
    Try(new Socket(LearningpathApiProperties.MetaServer, LearningpathApiProperties.MetaPort)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }
  def databaseIsAvailable: Boolean = Try(repository.configCount).isSuccess

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from learningpathapi_test.configtable;".execute.apply()(session)
      sql"delete from learningpathapi_test.configtable;".execute.apply()(session)
    })
  }

  override def beforeEach(): Unit = {
    repository = new ConfigRepository
    if (databaseIsAvailable) {
      emptyTestDatabase
    }
  }

  override def beforeAll(): Unit = {
    val datasource = DataSource.getHikariDataSource
    if (serverIsListenning) {
      DBMigrator.migrate(datasource)
      ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
    }
  }

  test("That updating configKey from empty database inserts config") {
    assume(databaseIsAvailable, "Database is unavailable")

    val newConfig = ConfigMeta(
      key = ConfigKey.IsWriteRestricted,
      value = "true",
      updatedAt = new Date(0),
      updatedBy = "ndlaUser1"
    )

    repository.updateConfigParam(newConfig)

    repository.configCount should be(1)
    repository.getConfigWithKey(ConfigKey.IsWriteRestricted) should be(Some(newConfig))
  }

  test("That updating config works as expected") {
    assume(databaseIsAvailable, "Database is unavailable")

    val originalConfig = ConfigMeta(
      key = ConfigKey.IsWriteRestricted,
      value = "true",
      updatedAt = new Date(0),
      updatedBy = "ndlaUser1"
    )

    repository.updateConfigParam(originalConfig)
    repository.configCount should be(1)
    repository.getConfigWithKey(ConfigKey.IsWriteRestricted) should be(Some(originalConfig))

    val updatedConfig = ConfigMeta(
      key = ConfigKey.IsWriteRestricted,
      value = "false",
      updatedAt = new Date(10000),
      updatedBy = "ndlaUser2"
    )

    repository.updateConfigParam(updatedConfig)
    repository.configCount should be(1)
    repository.getConfigWithKey(ConfigKey.IsWriteRestricted) should be(Some(updatedConfig))
  }
}
