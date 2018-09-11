/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration
import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.NdlaClient
import scalaj.http.{Http, HttpRequest}
import no.ndla.learningpathapi.LearningpathApiProperties.SearchApiHost
import no.ndla.learningpathapi.model.domain._
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.write

import scala.util.Try

trait SearchApiClient {
  this: NdlaClient =>
  val searchApiClient: SearchApiClient

  class SearchApiClient extends LazyLogging {
    private val IndexTimeout = 90 * 1000 // 90 seconds
    implicit val formats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(LearningPathStatus) +
        new EnumNameSerializer(LearningPathVerificationStatus) +
        new EnumNameSerializer(StepType) +
        new EnumNameSerializer(StepStatus) +
        new EnumNameSerializer(EmbedType)

    def deleteLearningPathDocument(id: Long): Try[_] = {
      val req = Http(s"http://$SearchApiHost/intern/learningpath/$id")
        .method("DELETE")
        .timeout(IndexTimeout, IndexTimeout)

      ndlaClient.fetch[Any](req)
    }

    def indexLearningPathDocument(document: LearningPath): Try[_] = {
      val body = write(document)

      val req = Http(s"http://$SearchApiHost/intern/learningpath/")
        .method("POST")
        .header("Content-Type", "application/json")
        .postData(body)
        .timeout(IndexTimeout, IndexTimeout)

      ndlaClient.fetch[Any](req)
    }
  }

}
