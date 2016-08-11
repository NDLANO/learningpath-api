/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import no.ndla.learningpathapi.UnitSuite

class V4__RenameTagToTagsTest extends UnitSuite {

  val migration = new V4__RenameTagToTags()

  test("That convertingToNewFormat with no tags returns empty taglist") {
    val learningPath = V4_DBLearningPath(1,"""{"tags":[]}""")
    val optConverted = migration.convertTagsToNewFormat(learningPath)

    optConverted.isDefined should be(true)
    optConverted.get.document should equal(learningPath.document)
  }

  test("That converting an already converted learningPath returns none") {
    val learningPath = V4_DBLearningPath(2,"""{"tags":[{"tags": ["eple", "banan"], "language": "nb"}, {"tags": ["apple", "banana"], "language": "en"}]}""")
    migration.convertTagsToNewFormat(learningPath) should be(None)
  }

  test("That convertingToNewFormat converts to expected format") {
    val before = """{"tags":[{"tag":["eple","banan"],"language":"nb"},{"tag":["apple","banana"],"language":"en"}]}"""
    val expectedAfter = """{"tags":[{"tags":["eple","banan"],"language":"nb"},{"tags":["apple","banana"],"language":"en"}]}"""
    val learningPath = V4_DBLearningPath(3, before)

    val optConverted = migration.convertTagsToNewFormat(learningPath)
    optConverted.isDefined should be(true)
    optConverted.get.document should equal(expectedAfter)
  }

}
