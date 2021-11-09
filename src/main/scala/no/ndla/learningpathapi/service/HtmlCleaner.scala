/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.LearningpathApiProperties.BasicHtmlTags
import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.Entities
import org.jsoup.safety.Whitelist

object HtmlCleaner {

  def cleanHtml(text: String, allowHtml: Boolean): String = {
    val withoutHeaders = changeHeadersToStrongWrappedInP(text)
    allowHtml match {
      case true =>
        Jsoup.clean(withoutHeaders,
                    "",
                    new Whitelist().addTags(BasicHtmlTags: _*),
                    new OutputSettings().prettyPrint(false).charset("ascii"))
      case false =>
        Jsoup.clean(withoutHeaders,
                    "",
                    Whitelist.none(),
                    new OutputSettings()
                      .prettyPrint(false)
                      .escapeMode(Entities.EscapeMode.xhtml))
    }
  }

  def changeHeadersToStrongWrappedInP(text: String): String = {
    val document = Jsoup.parseBodyFragment(text)
    document.select("h1").tagName("strong").wrap("<p>")
    document.select("h2").tagName("strong").wrap("<p>")
    document.select("h3").tagName("strong").wrap("<p>")
    document.select("h4").tagName("strong").wrap("<p>")
    document.select("h5").tagName("strong").wrap("<p>")
    document.select("h6").tagName("strong").wrap("<p>")
    document.outputSettings().prettyPrint(false)
    document.body().html()
  }

}
