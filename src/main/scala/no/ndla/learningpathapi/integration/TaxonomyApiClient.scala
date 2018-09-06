/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.LearningpathApiProperties.ApiGatewayHost
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}
import scalaj.http.Http

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxononyApiClient: TaxonomyApiClient

  class TaxonomyApiClient {
    implicit val formats = org.json4s.DefaultFormats
    private val TaxonomyApiEndpoint = s"http://$ApiGatewayHost/taxonomy/v1"

    def getResource(nodeId: String): Try[TaxonomyResource] = {
      val resourceId = s"urn:resource:1:$nodeId"
      get[TaxonomyResource](s"$TaxonomyApiEndpoint/resources/$resourceId") match {
        case Failure(ex) =>
          Failure(ex)
        case Success(a) =>
          Success(a)
      }
    }

    def updateResource(resource: TaxonomyResource): Try[TaxonomyResource] = {
      put[String, TaxonomyResource](s"$TaxonomyApiEndpoint/resources/${resource.id}", resource) match {
        case Success(_) => Success(resource)
        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.is2xx) =>
          Success(resource)
        case Failure(ex) => Failure(ex)
      }
    }

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(url).params(params))
    }

    private def put[A, B <: AnyRef](url: String, data: B, params: (String, String)*)(
        implicit mf: Manifest[A],
        format: org.json4s.Formats): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        Http(url)
          .postData(write(data))
          .method("put")
          .header("content-type", "application/json")
          .params(params.toMap)
      )
    }

  }
}

case class TaxonomyResource(id: String, name: String, contentUri: Option[String], path: String)
