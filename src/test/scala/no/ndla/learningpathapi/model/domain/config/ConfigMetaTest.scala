/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain.config

import no.ndla.learningpathapi.{TestData, UnitSuite, UnitTestEnvironment}

class ConfigMetaTest extends UnitSuite with UnitTestEnvironment {

  test("That validation exists for all configuration parameters") {
    ConfigKey.values.map(key => {
      try {
        ConfigMeta(
          key = key,
          value = "",
          updatedAt = TestData.today,
          updatedBy = "OneCoolKid"
        ).validate
      } catch {
        case _: Throwable =>
          fail(
            s"Every ConfigKey value needs to be validated. '${key.toString}' threw an exception when attempted validation.")
      }
    })
  }

}
