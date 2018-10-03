/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V8__UpdateLicensesTest extends UnitSuite with TestEnvironment {
  val migration = new V8__UpdateLicenses

  test("migration should update to new learningpath status format") {
    {
      val old =
        s"""{"copyright":{"license":"by","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningPathDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":"by-sa","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningPathDocument(old) should equal(expected)

    }
    {
      val old =
        s"""{"copyright":{"license":"by-nc-nd","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-NC-ND-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningPathDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":"copyrighted","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"COPYRIGHTED","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningPathDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":"cc0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC0-1.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningPathDocument(old) should equal(expected)
    }
  }

  test("migration not do anything if the learningpath document already has new status format") {
    val original =
      s"""{"copyright":{"license":"COPYRIGHTED","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertLearningStepDocument(original) should equal(original)
  }

  test("migration should update to new learningstep status format") {
    {
      val old =
        s"""{"license":"by","title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"license":"CC-BY-4.0","title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningStepDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"license":"by-sa","title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"license":"CC-BY-SA-4.0","title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningStepDocument(old) should equal(expected)

    }
    {
      val old =
        s"""{"license":"by-nc-nd","title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"license":"CC-BY-NC-ND-4.0","title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningStepDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"license":"copyrighted","title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"license":"COPYRIGHTED","title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningStepDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"license":"cc0","title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"license":"CC0-1.0","title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertLearningStepDocument(old) should equal(expected)
    }
  }

  test("migration not do anything if the learningstep document already has new status format") {
    val original =
      s"""{"license":"COPYRIGHTED","title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertLearningStepDocument(original) should equal(original)
  }

}
