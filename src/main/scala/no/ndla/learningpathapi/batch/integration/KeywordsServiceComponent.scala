package no.ndla.learningpathapi.batch.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model.LearningPathTag
import no.ndla.mapping.ISO639Mapping.get6391CodeFor6392Code
import org.json4s.native.Serialization._

import scala.util.matching.Regex
import scalaj.http.{Http, HttpRequest}

trait KeywordsServiceComponent extends LazyLogging {
  val keywordsService: KeywordsService

  class KeywordsService {
    val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"
    val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

    def forNodeId(nid: Long): List[LearningPathTag] = {
      forRequest(Http(s"$TopicAPIUrl$nid"))
    }

    def forRequest(request: HttpRequest): List[LearningPathTag] = {
      implicit val formats = org.json4s.DefaultFormats

      val response = request.asString
      response.isError match {
        case true => {
          logger.error(s"Received error ${response.code} = ${response.statusLine} for url ${request.url}")
          List()
        }
        case false => {
          try {
            read[Keywords](response.body)
              .keyword
              .flatMap(_.names)
              .flatMap(_.data)
              .flatMap(_.toIterable)
              .map(t => LearningPathTag(t._2.trim.toLowerCase, getISO639(t._1)))
          } catch {
            case e: Exception => {
              logger.error(s"Could not extract tags for request = ${request.url}. Error was ${e.getMessage}")
              List()
            }
          }
        }
      }
    }


    def getISO639(languageUrl: String): Option[String] = {
      Option(languageUrl) collect { case pattern(group) => group } match {
        case Some(x) => if (x == "language-neutral") None else get6391CodeFor6392Code(x)
        case None => None
      }
    }
  }
}

case class Keywords(keyword: List[Keyword])
case class Keyword(psi: Option[String], topicId: Option[String], visibility: Option[String], approved: Option[String], processState: Option[String], psis: List[String],
                   originatingSites: List[String], types: List[Any], names: List[KeywordName])
case class Type(typeId: String)
case class TypeName(isoLanguageCode: String)
case class KeywordName(wordclass: String, data: List[Map[String, String]])
case class KeywordNameName(isoLanguageCode: String)
