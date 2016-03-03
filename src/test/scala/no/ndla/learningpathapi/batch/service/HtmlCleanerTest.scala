package no.ndla.learningpathapi.batch.service

import no.ndla.learningpathapi.UnitSuite

class HtmlCleanerTest extends UnitSuite {

  test("That formatting html formats correct") {
    val htmlBefore = "<ul><li>element</li></ul>"
    val expectedAfter =
      """<ul>
        |  <li>element</li>
        |</ul>""".stripMargin

    val htmlAfter = HtmlCleaner.formatHtml(htmlBefore)
    htmlAfter should equal(expectedAfter)
  }

  test("That <h1>test</h1> is changed to <p><strong>test</strong></p>") {
    val htmlBefore = "<h1>test</h1>"
    val expectedAfter = "<p><strong>test</strong></p>"

    HtmlCleaner.changeHeadersToStrongWrappedInP(htmlBefore) should equal (expectedAfter)
  }
}
