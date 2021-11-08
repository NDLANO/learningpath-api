/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import no.ndla.learningpathapi.UnitSuite

class V4__ConvertStatusNotListedToPrivateTest extends UnitSuite {
  val migration = new V4__ConvertStatusNotListedToPrivate()

  test("That converting an learningPath status with NOT_LISTED is changed to PRIVATE") {
    val before = """{"status":"NOT_LISTED"}"""
    val expectedAfter = """{"status":"PRIVATE"}"""
    val learningPath = V4_DBLearningPath(1, before)

    val converted = migration.convertLearningPathStatus(learningPath)
    converted.document should equal(expectedAfter)
  }

}
