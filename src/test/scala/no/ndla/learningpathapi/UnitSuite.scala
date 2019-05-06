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
import org.scalatest.mockito.MockitoSugar
import scala.util.Properties.setProp

abstract class UnitSuite
    extends FunSuite
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with MockitoSugar
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  setProp("NDLA_ENVIRONMENT", "local")

  setProp("SEARCH_SERVER", "search-server")
  setProp("SEARCH_REGION", "some-region")
  setProp("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setProp("MIGRATION_HOST", "migration-api")
  setProp("MIGRATION_USER", "username")
  setProp("MIGRATION_PASSWORD", "password")
  setProp("SEARCH_INDEX_NAME", "learning-integration-test-index")

  setProp(PropertyKeys.MetaUserNameKey, "postgres")
  setProp(PropertyKeys.MetaPasswordKey, "hemmelig")
  setProp(PropertyKeys.MetaResourceKey, "postgres")
  setProp(PropertyKeys.MetaServerKey, "127.0.0.1")
  setProp(PropertyKeys.MetaPortKey, "5432")
  setProp(PropertyKeys.MetaSchemaKey, "learningpathapi_test")
}
