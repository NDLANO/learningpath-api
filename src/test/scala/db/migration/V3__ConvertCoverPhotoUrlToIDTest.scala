package db.migration

import no.ndla.learningpathapi.UnitSuite

class V3__ConvertCoverPhotoUrlToIDTest extends UnitSuite {
  val migration = new V3__ConvertCoverPhotoUrlToID()

  test("That convertCoverPhotoUrl with no coverPhoto returns None") {
    val learningPath = V3_DBLearningPath(1,"""{"tags":[]}""")
    val optConverted = migration.convertCoverPhotoUrl(learningPath)

    optConverted should be(None)
  }

  test("That converting an already converted learningPath returns None") {
    val learningPath = V3_DBLearningPath(2,"""{"coverPhotoId": "1"}""")
    migration.convertCoverPhotoUrl(learningPath) should be(None)
  }

  test("That convertCoverPhotoUrl converts to expected format") {
    val before = """{"coverPhotoMetaUrl": "http://localhost/image-api/v2/images/55"}"""
    val expectedAfter = """{"coverPhotoId":"55"}"""
    val learningPath = V3_DBLearningPath(3, before)

    val optConverted = migration.convertCoverPhotoUrl(learningPath)
    optConverted.isDefined should be(true)
    optConverted.get.document should equal(expectedAfter)
  }

}
