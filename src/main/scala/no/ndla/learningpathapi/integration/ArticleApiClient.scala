/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.ArticleApiHost
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException

import scala.util.{Failure, Success}
import scalaj.http.{Http, HttpRequest}

trait ArticleApiClient{
  this: NdlaClient =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient extends LazyLogging {
    private val ArticleImportTimeout = 30 * 1000 // 30 seconds
    private val ExternalId = ":external_id"
    private val importArticleEndpoint = s"http://$ArticleApiHost/intern/import/$ExternalId"

    def importArticle(externalId: String): Option[ArticleImportStatus] =
      doRequest(Http(importArticleEndpoint.replace(ExternalId, externalId)).timeout(ArticleImportTimeout, ArticleImportTimeout).method("POST"))

    private def doRequest(httpRequest: HttpRequest): Option[ArticleImportStatus] = {
      ndlaClient.fetchWithForwardedAuth[ArticleImportStatus](httpRequest) match {
        case Success(metaInfo) => Some(metaInfo)
        case Failure(hre: HttpRequestException) => if (hre.is404) None else throw hre
        case Failure(ex: Throwable) => throw ex
      }
    }
  }

}
case class ArticleImportStatus(messages: Seq[String], visitedNodes: Seq[String], articleId: Option[Long])
