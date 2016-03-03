package no.ndla.learningpathapi.batch.service

import no.ndla.learningpathapi.UnitSuite

class HtmlCleanerTest extends UnitSuite {

  test("That <h1>test</h1> is changed to <p><strong>test</strong></p>") {
    val htmlBefore = "<h1>test</h1>"
    val expectedAfter = "<p><strong>test</strong></p>"

    HtmlCleaner.changeHeadersToStrongWrappedInP(htmlBefore) should equal (expectedAfter)
  }

  test("That <a href='blah'>Lenke</a> is changed to Lenke") {
    val htmlBefore = "<a href='blah'>Lenke</a>"
    val expectedAfter = "Lenke"

    HtmlCleaner.cleanHtml(htmlBefore) should equal (expectedAfter)
  }
}
