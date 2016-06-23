package no.ndla.learningpathapi

import java.util.Date

import no.ndla.learningpathapi.model.api.{Author, LearningPathStatus, LearningPath}
import no.ndla.learningpathapi.model.domain.ValidationException


class ApiModelsTest extends UnitSuite {

  val testLearningPath = LearningPath(1, 1, None, List(), List(), "", List(), "", None, Some(1), "PUBLIC", "", new Date(),List(), Author("Forfatter", "Ukjent"), true)

  test("That LearningPath.isPrivate returns true for a private learningpath") {
    testLearningPath.copy(status = "PRIVATE").isPrivate should be(right = true)
  }

  test("That LearningPath.isPrivate returns false for a public learningpath") {
    testLearningPath.copy(status = "PUBLIC").isPrivate should be(right = false)
  }
}
