/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.learningpathapi.UnitSuite

class V7__MovePublishedExternToUnlistedTest extends UnitSuite {
  val migration = new V7__MovePublishedExternToUnlisted()

  test("extern learningpaths with PUBLISHED should be moved to UNLISTED") {
    val before = """{"duration":0,"status":"PUBLISHED","owner":"hmm","verificationStatus":"EXTERNAL"}"""
    val expected = """{"duration":0,"status":"UNLISTED","owner":"hmm","verificationStatus":"EXTERNAL"}"""

    migration.updateStatus(before) should equal(expected)
  }

  test("extern learningpaths with PRIVATE should stay PRIVATE") {
    val before = """{"duration":0,"status":"PRIVATE","owner":"hmm","verificationStatus":"EXTERNAL"}"""
    val expected = """{"duration":0,"status":"PRIVATE","owner":"hmm","verificationStatus":"EXTERNAL"}"""

    migration.updateStatus(before) should equal(expected)
  }

  test("CREATED_BY_NDLA learningpaths should stay PUBLISHED") {
    val before = """{"duration":0,"status":"PUBLISHED","owner":"hmm","verificationStatus":"CREATED_BY_NDLA"}"""
    val expected = """{"duration":0,"status":"PUBLISHED","owner":"hmm","verificationStatus":"CREATED_BY_NDLA"}"""

    migration.updateStatus(before) should equal(expected)
  }

}
