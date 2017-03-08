/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import java.util.Date

import no.ndla.learningpathapi.model.api.{Author, LearningPath, License, Copyright}

class ApiModelsTest extends UnitSuite {
  val bush = Author("author", "Low Energy’ Jeb")
  val license = License("pubslicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
  val copyright = Copyright(Some(license), List(bush))
  val testLearningPath = LearningPath(1, 1, None, List(), List(), "", List(), "", None, Some(1), "PUBLIC", "", new Date(),List(), Author("Forfatter", "Ukjent"), copyright, true)

  test("That LearningPath.isPrivate returns true for a private learningpath") {
    testLearningPath.copy(status = "PRIVATE").isPrivate should be(right = true)
  }

  test("That LearningPath.isPrivate returns false for a public learningpath") {
    testLearningPath.copy(status = "PUBLIC").isPrivate should be(right = false)
  }
}
