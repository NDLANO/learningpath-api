/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V11__CreatedByNdlaStatusForOwnersWithRolesTest extends UnitSuite with TestEnvironment {
  val migration = new V11__CreatedByNdlaStatusForOwnersWithRoles

  test("That migration changes verificationStatus correctly") {
    val old =
      s"""{"verificationStatus":"EXTERNAL","copyright":{"license":"by","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
    val expected =
      s"""{"verificationStatus":"CREATED_BY_NDLA","copyright":{"license":"by","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertLearningPathDocument(old) should be(expected)
  }

  test("That migration does not change anything if status is already CREATED_BY_NDLA") {
    val old =
      s"""{"verificationStatus":"CREATED_BY_NDLA","copyright":{"license":"by","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
    val expected =
      s"""{"verificationStatus":"CREATED_BY_NDLA","copyright":{"license":"by","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertLearningPathDocument(old) should be(expected)
  }

}
