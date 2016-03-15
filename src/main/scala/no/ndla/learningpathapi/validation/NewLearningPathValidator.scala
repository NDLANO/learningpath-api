package no.ndla.learningpathapi.validation

import com.netaporter.uri.Uri._
import no.ndla.learningpathapi._


class NewLearningPathValidator {
  val MISSING_DESCRIPTION = "At least one description is required."
  val INVALID_COVER_PHOTO = "The url to the coverPhoto must point to an image in NDLA Image API."

  val languageValidator = new LanguageValidator
  val noHtmlTextValidator = new TextValidator(allowHtml = false)
  val titleValidator = new TitleValidator
  val durationValidator = new DurationValidator

  def validate(newLearningPath: NewLearningPath): List[ValidationMessage] = {
    titleValidator.validate(newLearningPath.title) :::
      validateDescription(newLearningPath.description) :::
      validateDuration(newLearningPath.duration).toList :::
      validateCoverPhoto(newLearningPath.coverPhotoUrl).toList :::
      validateTags(newLearningPath.tags)
  }

  def validateDescription(descriptions: List[Description]): List[ValidationMessage] = {
    descriptions.isEmpty match {
      case true => List(ValidationMessage("description", MISSING_DESCRIPTION))
      case false => descriptions.flatMap(description => {
        noHtmlTextValidator.validate("description.description", description.description).toList :::
          languageValidator.validate("description.language", description.language).toList
      })
    }
  }

  def validateDuration(durationOpt: Option[Int]): Option[ValidationMessage] = {
    durationOpt match {
      case None => None
      case Some(duration) => durationValidator.validateRequired(durationOpt)
    }
  }

  def validateCoverPhoto(coverPhotoUrl: Option[String]): Option[ValidationMessage] = {
    coverPhotoUrl.flatMap(url => {
      val possibleImageApiDomains = "api.ndla.no" :: LearningpathApiProperties.Domains.toList
      val parsedUrl = parse(url)
      val host = parsedUrl.host.getOrElse("")

      val hostCorrect = possibleImageApiDomains.contains(host)
      val pathCorrect = parsedUrl.path.startsWith("/images")

      hostCorrect && pathCorrect match {
        case true => None
        case false => Some(ValidationMessage("coverPhotoUrl", INVALID_COVER_PHOTO))
      }
    })
  }

  def validateTags(tags: List[LearningPathTag]): List[ValidationMessage] = {
    tags.flatMap(tag => {
      noHtmlTextValidator.validate("tags.tag", tag.tag).toList :::
        languageValidator.validate("tags.language", tag.language).toList
    })
  }
}
