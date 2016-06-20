package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.domain.HttpRequestException

import scala.util.{Failure, Success}
import scalaj.http.Http


trait ImageApiClientComponent {
  this: NdlaClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient extends LazyLogging {
    val ExternalId = ":external_id"
    val byExternalIdEndpoint = s"http://${LearningpathApiProperties.ImageApiHost}/admin/extern/$ExternalId"

    def getImageMetaInformationForExternId(externalId: String): Option[ImageMetaInformation] = {
      ndlaClient.fetch[ImageMetaInformation](Http(byExternalIdEndpoint.replace(ExternalId, externalId))) match {
        case Success(metaInfo) => Some(metaInfo)
        case Failure(hre: HttpRequestException) => if (hre.is404) None else throw hre
        case Failure(ex: Throwable) => throw ex
      }
    }
  }
}

case class ImageMetaInformation(id:String, images:ImageVariants)
case class ImageVariants(small: Option[Image], full: Option[Image])
case class Image(url:String, size:Int, contentType:String)
