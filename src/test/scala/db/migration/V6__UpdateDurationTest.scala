/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.learningpathapi.UnitSuite

class V6__UpdateDurationTest extends UnitSuite {
  val migration = new V6__UpdateDuration()

  test("learningpaths with duration <= 0 should be set to 1") {
    val before = """{"duration":0,"status":"PUBLISHED","owner":"hmm"}"""
    val expected = """{"duration":1,"status":"PUBLISHED","owner":"hmm"}"""

    migration.updateDuration(before) should equal(expected)
  }

}
