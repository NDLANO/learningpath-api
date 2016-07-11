package db.migration

import no.ndla.learningpathapi.UnitSuite

class V5__AddCopyrightFieldToLearningPathTest extends UnitSuite {
  val migration = new V5__AddCopyrightFieldToLearningPath()

  test("That addCopyrightField adds a copyright field if it does not exist") {
    val before = """{"tags":[{"tags":[],"language":"nb"},{"tags":[],"language":"en"}]}"""
    val expectedAfter = """{"tags":[{"tags":[],"language":"nb"},{"tags":[],"language":"en"}],
        |"copyright":{"license":"by-sa","contributors":[]}}""".stripMargin.replace("\n", "")
    val learningPath = V5_DBLearningPath(3, before)

    val optConverted = migration.addCopyrightField(learningPath)

    optConverted.isDefined should be(true)
    optConverted.get.document should equal(expectedAfter)
  }

  test("That addCopyrightField returns None if the copyright field already exist") {
    val before = """{"tags":[{"tags":[],"language":"nb"},{"tags":[],"language":"en"}],
                   |"copyright":{"license":"by-sa","contributors":[]}}""".stripMargin
    val learningPath = V5_DBLearningPath(3, before)

    val optConverted = migration.addCopyrightField(learningPath)
    optConverted should be(None)
  }
}
