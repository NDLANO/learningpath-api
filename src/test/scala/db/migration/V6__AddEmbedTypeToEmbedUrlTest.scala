package db.migration

import no.ndla.learningpathapi.UnitSuite

/**
  * Created by chrpeter on 25/12/2016.
  */
class V6__AddEmbedTypeToEmbedUrlTest extends UnitSuite{
  val migration = new V6__AddEmbedTypeToEmbedUrl()

  test("That embedType is added to the embedurl object if exists") {
    val before = """{"embedUrl":[{"url":"http://api.test.ndla.no:8082/article/1","language":"nb"}]}""".stripMargin.replace("\n", "")
    val expectedAfter = """{"embedUrl":[{"url":"http://api.test.ndla.no:8082/article/1","language":"nb","embedType":"oembed"}]}""".stripMargin.replace("\n", "")
    val learningStep = V6_DBContent(3, before)

    val optConverted = migration.convertDocumentToNewFormat(learningStep)
    optConverted.isDefined should be(true)
    optConverted.get.document should equal(expectedAfter)
  }

}
