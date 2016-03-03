package no.ndla.learningpathapi.batch.service

import no.ndla.learningpathapi.LearningpathApiProperties.BasicHtmlTags
import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.safety.Whitelist

object HtmlCleaner {

  def cleanHtml(text: String): String = {
    val withoutHeaders = changeHeadersToStrongWrappedInP(text)
    Jsoup.clean(withoutHeaders, "", new Whitelist().addTags(BasicHtmlTags:_*), new OutputSettings().prettyPrint(false))
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
