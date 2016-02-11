package no.ndla.learningpathapi.batch

import no.ndla.learningpathapi.UnitSuite


class UriParserTest extends UnitSuite{

  test("Parse en uri") {
    val urls = List(
      "https://www.youtube.com/watch?v=ir2RqpOLaq8&feature=youtu.be",
      "http://red.ndla.no/node/148108",
      "http://www.vg.no",
      "http://red.ndla.no/nb/h5pcontent/150089?fag=27",
      "http://red.ndla.no/nb/node/150198?fag=27",
      "NULL"
    )

    urls.foreach(url => {
      println(s"YOUTUBE = $url, EMBED = ${getEmbedUri2(url)}")
    })
  }

  test("Tidy html") {
    val untidy =
      """
        |<p>
        |<strong>Brønnvæsker </strong>er et samlebegrep for væsker som brukes i brønnen etter at borefasen er avsluttet.
        |Væsken har som oppgave å holde trykkbalanse i brønnen samtidig som den skal unngå å skade reservoaret eller utstyr som er installert i brønnen.
        |</p>
        |<p>
        |Det er vanligst å bruke saltløsning (<em>brine</em>) i brønnen.
        |</p>
        |<p> </p>
        |<p>
        |De vanligste bruksområdene for brine er som vaskevæske etter borefasen, kompletteringsvæske i kompletteringsfasen,
        |<em>packerfluid</em>,
        |bærevæske for grus til gruspakkeoperasjon (<em>carrier fluid</em>), brønnservice-væske og til frakturering av reservoar<em> (fracking)</em>.
        |</p>
        |<p>Hovedmålet i dette emnet er at du kjenner til sammensetning og bruksområder for brønnvæsker, og forstår viktigheten av brønnkontroll i alle sammenhenger.
        |</p>
        |<p> </p><p><strong>Forkunnskaper</strong><br />Du bør forstå hvordan vi opprettholder trykkontroll i en brønn ved hjelp av hydrostatisk trykk. Du bør vite hvordan vi øker egenvekt i væsker og hvorfor vi gjør det. Du bør forstå hvordan væske sirkuleres inn i en brønn.</p><p> </p><h2>Læringsmål/delmål</h2><h3>Brønnvæsker</h3><ul>\t<li>Du skal beskrive ulike bruksområder og hensikten med kompletteringsvæskene.</li>\t<li>Du skal beskrive hovedbestanddelene i kompletteringsvæsken og hvilken oppgave tilsetningsstoffene har.</li>\t<li>Du skal forklare saltenes bruksområder (begrensninger) og hvordan man kan justere vekt og viskositet.</li>\t<li>Du skal vurdere helsefarer og miljørisiko i forbindelse med bruk av disse stoffene.</li></ul><p> </p><p>Det er lagt til et vaskeprogram (<em>clean up guide</em>) på engelsk som kan brukes som fordypning i emnet, til yrkesrettet engelsk (FYR), eller for prosjekt til fordypning.</p>
      """.stripMargin

    println("UNTIDY:")
    println(untidy)
    println("---------------------------------------------------------------------------------------------------------------------")
    println("TIDY:")
    println(untidy.replaceAll("<(.*?)>", " "))
  }

  def getEmbedUri2(uri: String): String = {
    val parsedUri = com.netaporter.uri.Uri.parse(uri)
    parsedUri.host match {
      case None => uri
      case Some(host) => {
        host match {
          case h if h == "red.ndla.no" => s"http://ndla.no${parsedUri.path}/oembed"
          case h if h == "www.youtube.com" => {
            parsedUri.query.param("v") match {
              case Some(videoId) => s"https://www.youtube.com/embed/$videoId"
              case None => uri
            }
          }
          case default => uri
        }
      }
    }
  }

  def getEmbedUri(uri: String): String = {
    val parsedUri = com.netaporter.uri.Uri.parse(uri)
    parsedUri.host match {
      case None => uri
      case Some(host) => {
        if(host == "red.ndla.no") {
          s"http://ndla.no${parsedUri.path}/oembed"
        } else if (host == "www.youtube.com"){
          parsedUri.query.param("v") match {
            case Some(videoId) => s"https://www.youtube.com/embed/$videoId"
            case None => uri
          }
        } else {
          uri
        }
      }
    }
  }

}
