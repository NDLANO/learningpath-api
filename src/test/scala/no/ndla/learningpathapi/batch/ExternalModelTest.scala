package no.ndla.learningpathapi.batch

import no.ndla.learningpathapi.UnitSuite


class ExternalModelTest extends UnitSuite {

  test("That Step.embedUrlToNdlaNo converts None to None") {
    Step(1,1,1,"",1,1, None, None, "").embedUrlToNdlaNo should equal(None)
  }

  test("That Step.embedUrlToNdlaNo converts no-host to None") {
    val noHostUrl = Some("ingenhost")
    Step(1,1,1,"",1,1, noHostUrl, None, "").embedUrlToNdlaNo should equal(None)
  }

  test("That Step.embedUrlToNdlaNo converts red.ndla.no to ndla.no") {
    val redUrl = Some("http://red.ndla.no/node/145805")
    val expectedUrl = Some("http://ndla.no/node/145805/oembed")

    Step(1, 1, 1, "", 1, 1, redUrl, None, "")
      .embedUrlToNdlaNo should equal(expectedUrl)
  }

  test("That Step.embedUrlToNdlaNo converts www.youtube.com with v-param to youtube-embed url") {
    val youtubeUrl = Some("http://www.youtube.com/?v=AnAwesomeVideo")
    val expectedUrl = Some("https://www.youtube.com/embed/AnAwesomeVideo")

    Step(1,1,1,"",1,1,youtubeUrl,None,"").embedUrlToNdlaNo should equal(expectedUrl)
  }

  test("That Step.embedUrlToNdlaNo returns same youtube-url if no v-param") {
    val youtubeUrl, expectedUrl = Some("http://www.youtube.com/AnAwesomeVideo")
    Step(1,1,1,"",1,1,youtubeUrl,None,"").embedUrlToNdlaNo should equal(expectedUrl)
  }

  test("That Step.embedUrlToNdlaNo returns other than red.ndla.no and youtube-urls as is") {
    val urlToCnn, expectedUrl = Some("http://www.cnn.com/")
    Step(1,1,1,"",1,1,urlToCnn,None,"").embedUrlToNdlaNo should equal(expectedUrl)
  }

  test("That Node.isTranslation returns false for tnid equal to nid") {
    Node(1,1,"","",1,None).isTranslation should be(right = false)
  }

  test("That Node.isTranslation returns false for tnid equal to 0") {
    Node(1,0,"","",1,None).isTranslation should be(right = false)
  }

  test("That Node.isTranslation returns false for tnid unequal to nid") {
    Node(1,2,"","",1,None).isTranslation should be(right = true)
  }
}
