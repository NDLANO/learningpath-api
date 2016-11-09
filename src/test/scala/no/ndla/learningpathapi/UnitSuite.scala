/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.network.secrets.PropertyKeys
import org.scalatest._
import org.scalatest.mock.MockitoSugar

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {

  setEnv("NDLA_ENVIRONMENT", "local")

  setEnv(PropertyKeys.MetaUserNameKey, "user")
  setEnv(PropertyKeys.MetaPasswordKey, "password")
  setEnv(PropertyKeys.MetaResourceKey, "dbresource")
  setEnv(PropertyKeys.MetaServerKey, "dbserver")
  setEnv(PropertyKeys.MetaPortKey, "1")
  setEnv(PropertyKeys.MetaSchemaKey, "learningpathapi")

  setEnv("SEARCH_SERVER", "search-server")
  setEnv("SEARCH_REGION", "some-region")
  setEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setEnv("MIGRATION_HOST", "migration-api")
  setEnv("MIGRATION_USER", "username")
  setEnv("MIGRATION_PASSWORD", "password")

  def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }
}