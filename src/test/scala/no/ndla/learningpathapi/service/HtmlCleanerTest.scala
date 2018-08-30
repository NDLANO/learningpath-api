/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.UnitSuite

class HtmlCleanerTest extends UnitSuite {

  test("That <h1>test</h1> is changed to <p><strong>test</strong></p>") {
    val htmlBefore = "<h1>test</h1>"
    val expectedAfter = "<p><strong>test</strong></p>"

    HtmlCleaner.changeHeadersToStrongWrappedInP(htmlBefore) should equal(
      expectedAfter)
  }

  test("That <a href='blah'>Lenke</a> is changed to Lenke") {
    val htmlBefore = "<a href='blah'>Lenke</a>"
    val expectedAfter = "Lenke"

    HtmlCleaner.cleanHtml(htmlBefore, allowHtml = true) should equal(
      expectedAfter)
  }

  test("That <strong>heisann</strong> is changed to heisann") {
    val htmlBefore = "<strong>heisann</strong>"
    val expectedAfter = "heisann"
    HtmlCleaner.cleanHtml(htmlBefore, allowHtml = false) should equal(
      expectedAfter)
  }

  test("That the norwegian characters æ,ø and å is escaped in html-mode") {
    val textBefore = "æ, ø og å"
    val expectedAfter = "&aelig;, &oslash; og &aring;"

    HtmlCleaner.cleanHtml(textBefore, allowHtml = true) should equal(
      expectedAfter)
  }

  test("That the norwegian characters æ,ø and å is kept as is in nonhtml-mode") {
    val textBefore = "æ, ø og å"
    HtmlCleaner.cleanHtml(textBefore, allowHtml = false) should equal(
      textBefore)
  }
}
