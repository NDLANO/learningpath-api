/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import java.util.Date
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
import no.ndla.learningpathapi.{DBMigrator, LearningpathApiProperties, TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite
import no.ndla.network.secrets.PropertyKeys
import no.ndla.tag.IntegrationTest
import org.scalatest.Outcome
import scalikejdbc.{DB, _}

import scala.util.{Failure, Try}
import scala.util.Properties.setProp

@IntegrationTest
class ConfigRepositoryTest
    extends IntegrationSuite(EnablePostgresContainer = true, schemaName = "learningpathapi_test")
    with UnitSuite
    with TestEnvironment {

  override val dataSource = testDataSource.get

  var repository: ConfigRepository = _

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    postgresContainer match {
      case Failure(ex) =>
        println(s"Postgres container not running, cancelling '${this.getClass.getName}'")
        println(s"Got exception: ${ex.getMessage}")
        ex.printStackTrace()
      case _ =>
    }
    if (!sys.env.getOrElse("CI", "false").toBoolean) {
      assume(postgresContainer.isSuccess, "Docker environment unavailable for postgres container")
    }
    super.withFixture(test)
  }

  def databaseIsAvailable: Boolean = {
    val res = Try(repository.configCount)
    res.isSuccess
  }

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from configtable;".execute()(session)
      sql"delete from configtable;".execute()(session)
    })
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Try(ConnectionPool.singleton(new DataSourceConnectionPool(dataSource)))
    Try(DBMigrator.migrate(dataSource))
  }

  override def beforeEach(): Unit = {
    repository = new ConfigRepository
    if (databaseIsAvailable) {
      emptyTestDatabase
    }
  }

  test("That updating configKey from empty database inserts config") {
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
