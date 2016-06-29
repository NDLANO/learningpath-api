package db.migration

import no.ndla.learningpathapi.UnitSuite

class V3__ChangeTagStructureTest extends UnitSuite {

  val migration = new V3__ChangeTagStructure()

  test("That convertingToNewFormat with no tags returns empty taglist") {
    val learningPath = DBLearningPath(1,"""{"tags":[]}""")
    val optConverted = migration.convertTagsToNewFormat(learningPath)

    optConverted.isDefined should be(true)
    optConverted.get.document should equal(learningPath.document)
  }

  test("That converting an already converted learningPath returns none") {
    val learningPath = DBLearningPath(2,"""{"tags":[{"tag": ["eple", "banan"], "language": "nb"}, {"tag": ["apple", "banana"], "language": "en"}]}""")
    migration.convertTagsToNewFormat(learningPath) should be(None)
  }

  test("That convertingToNewFormat converts to expected format") {
    val before = """{"tags": [{"tag": "eple", "language":"nb"}, {"tag": "banan", "language":"nb"}, {"tag": "apple", "language":"en"}, {"tag": "banana", "language":"en"}]}"""
    val expectedAfter = """{"tags":[{"tag":["eple","banan"],"language":"nb"},{"tag":["apple","banana"],"language":"en"}]}"""
    val learningPath = DBLearningPath(3, before)

    val optConverted = migration.convertTagsToNewFormat(learningPath)
    optConverted.isDefined should be(true)
    optConverted.get.document should equal(expectedAfter)
  }
}
