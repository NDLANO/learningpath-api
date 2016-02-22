package no.ndla.learningpathapi

import java.util.Date

import no.ndla.learningpathapi.model.ValidationException


class ApiModelsTest extends UnitSuite {

  test("That LearningPathStatus.validate throws exception for unknown status") {
    val status = "Ikke gyldig"
    assertResult(s"'$status' is not a valid publishingstatus.") {
      intercept[ValidationException] {LearningPathStatus(status).validate()}.getMessage
    }
  }

  test("That LearningPathStatus.validate exits normally for known status") {
    LearningPathStatus("PUBLISHED").validate()
  }

  test("That LearningPath.isPrivate returns true for a private learningpath") {
    LearningPath(1, List(), List(), "", List(), "", None, 1, "PRIVATE", "", new Date(),List(), Author("Forfatter", "Ukjent")).isPrivate should be(right = true)
  }

  test("That LearningPath.isPrivate returns false for a public learningpath") {
    LearningPath(1, List(), List(), "", List(), "", None, 1, "PUBLIC", "", new Date(),List(), Author("Forfatter", "Ukjent")).isPrivate should be(right = false)
  }
}
