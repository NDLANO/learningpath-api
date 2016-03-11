package no.ndla.learningpathapi.validation

import com.netaporter.uri.Uri._
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.api.ValidationMessage

trait CoverPhotoValidatorComponent {
  val coverPhotoValidator: CoverPhotoValidator

  class CoverPhotoValidator {
    val INVALID_TARGET = "The url to the coverPhoto must point to an image in NDLA Image API."

    def validate(coverPhotoUrl: Option[String]): Option[ValidationMessage] = {
      coverPhotoUrl match {
        case Some(url) => if(pointsToImageApi(url)) None else Some(ValidationMessage("coverPhotoUrl", INVALID_TARGET))
        case None => None
      }
    }

    def pointsToImageApi(url: String): Boolean = {
      val possibleImageApiDomains = "api.ndla.no" :: LearningpathApiProperties.Domains.toList
      val parsedUrl = parse(url)
      val host = parsedUrl.host.getOrElse("")

      val hostCorrect = possibleImageApiDomains.contains(host)
      val pathCorrect = parsedUrl.path.startsWith("/images")

      hostCorrect && pathCorrect
    }
  }
}
