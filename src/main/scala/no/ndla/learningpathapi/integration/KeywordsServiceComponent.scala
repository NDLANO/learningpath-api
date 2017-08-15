/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import org.json4s.native.Serialization._

import scala.util.matching.Regex
import scalaj.http.{Http, HttpRequest}
import no.ndla.learningpathapi.model.domain.{Language, LearningPathTags}
import no.ndla.mapping.ISO639.get6391CodeFor6392Code

trait KeywordsServiceComponent extends LazyLogging {
  val keywordsService: KeywordsService

  class KeywordsService {
    val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"
    val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

    def forNodeId(nid: Long): Seq[LearningPathTags] = {
      forRequest(Http(s"$TopicAPIUrl$nid"))
    }

    def forRequest(request: HttpRequest): Seq[LearningPathTags] = {
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
              .map(t => (getISO639(t._1), t._2.trim.toLowerCase))
              .groupBy(_._1).map(entry => (entry._1, entry._2.map(_._2)))
              .map(entr => LearningPathTags(entr._2, Language.languageOrUnknown(entr._1))).toList
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
