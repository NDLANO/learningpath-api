/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import java.util.Date

import no.ndla.learningpathapi.model.api._

class ApiModelsTest extends UnitSuite {
  val bush = Author("author", "Low Energy’ Jeb")
  val license = License("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
  val copyright = Copyright(license, List(bush))

  val testLearningPath = LearningPathV2(
    1,
    1,
    None,
    Title("Tittel", "nb"),
    Description("Beskrivelse", "nb"),
    "",
    List(),
    "",
    None,
    Some(1),
    "PUBLIC",
    "",
    new Date(),
    LearningPathTags(List(), "nb"),
    copyright,
    true,
    Seq("nb")
  )

  test("That LearningPath.isPrivate returns true for a private learningpath") {
    testLearningPath.copy(status = "PRIVATE").isPrivate should be(right = true)
  }

  test("That LearningPath.isPrivate returns false for a public learningpath") {
    testLearningPath.copy(status = "PUBLIC").isPrivate should be(right = false)
  }
}
