package no.ndla.learningpathapi

import java.util.Date

import no.ndla.learningpathapi.model.api.{Author, LearningPathStatus, LearningPath}
import no.ndla.learningpathapi.model.domain.ValidationException


class ApiModelsTest extends UnitSuite {

  val testLearningPath = LearningPath(1, 1, List(), List(), "", List(), "", None, Some(1), "PUBLIC", "", new Date(),List(), Author("Forfatter", "Ukjent"))

  test("That LearningPathStatus.validate throws exception for unknown status") {
    val status = "Ikke gyldig"
    assertResult(s"'$status' is not a valid publishingstatus.") {
      intercept[ValidationException] {LearningPathStatus(status).validate()}.errors.head.message
    }
  }

  test("That LearningPathStatus.validate exits normally for known status") {
    val learningPathStatus = LearningPathStatus("PUBLISHED")
    learningPathStatus.validate() should equal(learningPathStatus)
  }

  test("That LearningPath.isPrivate returns true for a private learningpath") {
    testLearningPath.copy(status = "PRIVATE").isPrivate should be(right = true)
  }

  test("That LearningPath.isPrivate returns false for a public learningpath") {
    testLearningPath.copy(status = "PUBLIC").isPrivate should be(right = false)
  }
}
