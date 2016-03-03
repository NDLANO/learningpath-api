package no.ndla.learningpathapi.batch.service

import no.ndla.learningpathapi.LearningpathApiProperties.BasicHtmlTags
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

object HtmlCleaner {

  def cleanHtml(text: String): String = {
    val withoutHeaders = changeHeadersToStrongWrappedInP(text)
    val formatted = formatHtml(withoutHeaders)
    Jsoup.clean(formatted, new Whitelist().addTags(BasicHtmlTags:_*))
  }

  def formatHtml(text: String): String = {
    val document = Jsoup.parseBodyFragment(text)
    document.outputSettings().indentAmount(2).prettyPrint(true)
    document.body().html()
  }

  def changeHeadersToStrongWrappedInP(text: String): String = {
    val document = Jsoup.parseBodyFragment(text)
    document.select("h1").tagName("strong").wrap("<p>")
    document.select("h2").tagName("strong").wrap("<p>")
    document.select("h3").tagName("strong").wrap("<p>")
    document.select("h4").tagName("strong").wrap("<p>")
    document.select("h5").tagName("strong").wrap("<p>")
    document.select("h6").tagName("strong").wrap("<p>")

    document.body().html()
  }

}
