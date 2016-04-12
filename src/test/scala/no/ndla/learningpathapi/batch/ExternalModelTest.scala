package no.ndla.learningpathapi.batch

import no.ndla.learningpathapi.UnitSuite


class ExternalModelTest extends UnitSuite {

  val testStep = Step(
    packageId = 1,
    pageId = 1,
    pos = 1,
    title = "",
    stepType = 1,
    pageAuthor = 1,
    embedUrl = None,
    description = None,
    language = "")

  val testNode = Node(
    nid = 1,
    tnid = 1,
    language = "",
    title = "",
    packageId = 1,
    imageNid = None,
    description = "")

  test("That Step.embedUrlToNdlaNo converts None to None") {
    testStep.copy(embedUrl = None).embedUrlToNdlaNo should equal(None)
  }

  test("That Step.embedUrlToNdlaNo converts no-host to None") {
    val noHostUrl = Some("ingenhost")
    testStep.copy(embedUrl = noHostUrl).embedUrlToNdlaNo should equal(None)
  }

  test("That Step.embedUrlToNdlaNo converts red.ndla.no to ndla.no") {
    val redUrl = Some("http://red.ndla.no/node/145805")
    val expectedUrl = Some("http://ndla.no/node/145805")

    testStep.copy(embedUrl = redUrl).embedUrlToNdlaNo should equal(expectedUrl)
  }

  test("That Step.embedUrlToNdlaNo returns other than red.ndla.no as is") {
    val urlToCnn, expectedUrl = Some("http://www.cnn.com/")
    testStep.copy(embedUrl = urlToCnn).embedUrlToNdlaNo should equal(expectedUrl)
  }

  test("That Node.isTranslation returns false for tnid equal to nid") {
    testNode.copy(nid = 123, tnid = 123).isTranslation should be(right = false)
  }

  test("That Node.isTranslation returns false for tnid equal to 0") {
    testNode.copy(tnid = 0).isTranslation should be(right = false)
  }

  test("That Node.isTranslation returns false for tnid unequal to nid") {
    testNode.copy(nid = 1, tnid = 2).isTranslation should be(right = true)
  }
}
