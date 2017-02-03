package db.migration

import no.ndla.learningpathapi.UnitSuite

class V4__ConvertStatusNotListedToPrivateTest extends UnitSuite {
  val migration = new V4__ConvertStatusNotListedToPrivate()

  test("That converting an learningPath status with PRIVATE does not change") {
    val before = """{"status":"PRIVATE"}"""
    val expectedAfter = """{"status":"PRIVATE"}"""
    val learningPath = V4_DBLearningPath(1,before)

    val converted = migration.convertLearningPathStatus(learningPath)
    converted.document should be(expectedAfter)
  }

  test("That converting an learningPath status with PUBLIC does not change") {
    val before = """{"status":"PUBLISHED"}"""
    val expectedAfter = """{"status":"PUBLISHED"}"""
    val learningPath = V4_DBLearningPath(1,before)

    val converted = migration.convertLearningPathStatus(learningPath)
    converted.document should be(expectedAfter)
  }

  test("That converting an learningPath status with NOT_LISTED is changed to PRIVATE") {
    val before = """{"status":"NOT_LISTED"}"""
    val expectedAfter = """{"status":"PRIVATE"}"""
    val learningPath = V4_DBLearningPath(1, before)

    val converted = migration.convertLearningPathStatus(learningPath)
    converted.document should equal(expectedAfter)
  }

}
