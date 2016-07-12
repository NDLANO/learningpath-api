package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException

import scala.util.{Failure, Success}
import scalaj.http.{Http, HttpRequest}


trait ImageApiClientComponent {
  this: NdlaClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient extends LazyLogging {
    val ExternalId = ":external_id"
    val byExternalIdEndpoint = s"http://${LearningpathApiProperties.ImageApiHost}/intern/extern/$ExternalId"
    val importImageEndpoint = s"http://${LearningpathApiProperties.ImageApiHost}/intern/import/$ExternalId"

    def imageMetaWithExternalId(externalId: String): Option[ImageMetaInformation] = doRequest(Http(byExternalIdEndpoint.replace(ExternalId, externalId)))

    def imageMetaOnUrl(url: String): Option[ImageMetaInformation] = doRequest(Http(url))

    def importImage(externalId: String): Option[ImageMetaInformation] = doRequest(Http(importImageEndpoint.replace(ExternalId, externalId)).method("POST"))

    private def doRequest(httpRequest: HttpRequest): Option[ImageMetaInformation] = {
      ndlaClient.fetch[ImageMetaInformation](httpRequest) match {
        case Success(metaInfo) => Some(metaInfo)
        case Failure(hre: HttpRequestException) => if (hre.is404) None else throw hre
        case Failure(ex: Throwable) => throw ex
      }
    }
  }

}

case class ImageMetaInformation(id: String, metaUrl: String, images: ImageVariants)

case class ImageVariants(small: Option[Image], full: Option[Image])

case class Image(url: String, size: Int, contentType: String)
