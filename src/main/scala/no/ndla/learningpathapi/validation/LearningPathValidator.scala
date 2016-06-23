package no.ndla.learningpathapi.validation

import com.netaporter.uri.Uri._
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain.{Description, LearningPath, LearningPathTag}


class LearningPathValidator(titleRequired: Boolean = true, descriptionRequired: Boolean = true) {

  val MISSING_DESCRIPTION = "At least one description is required."
  val INVALID_COVER_PHOTO = "The url to the coverPhoto must point to an image in NDLA Image API."

  val languageValidator = new LanguageValidator
  val noHtmlTextValidator = new TextValidator(allowHtml = false)
  val titleValidator = new TitleValidator(titleRequired)
  val durationValidator = new DurationValidator

  def validate(newLearningPath: LearningPath): Seq[ValidationMessage] = {
    titleValidator.validate(newLearningPath.title) ++
      validateDescription(newLearningPath.description) ++
      validateDuration(newLearningPath.duration).toList ++
      validateCoverPhoto(newLearningPath.coverPhotoMetaUrl).toList ++
      validateTags(newLearningPath.tags)
  }

  def validateDescription(descriptions: Seq[Description]): Seq[ValidationMessage] = {
    (descriptionRequired, descriptions.isEmpty) match {
      case (false, true) => List()
      case (true, true) => List(ValidationMessage("description", MISSING_DESCRIPTION))
      case (_, false) => descriptions.flatMap(description => {
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

  def validateCoverPhoto(coverPhotoMetaUrl: Option[String]): Option[ValidationMessage] = {
    coverPhotoMetaUrl.flatMap(url => {
      val possibleImageApiDomains = "api.ndla.no" :: LearningpathApiProperties.Domains.toList
      val parsedUrl = parse(url)
      val host = parsedUrl.host.getOrElse("")

      val hostCorrect = possibleImageApiDomains.contains(host)
      val pathCorrect = parsedUrl.path.startsWith("/images")

      hostCorrect && pathCorrect match {
        case true => None
        case false => Some(ValidationMessage("coverPhotoMetaUrl", INVALID_COVER_PHOTO))
      }
    })
  }

  def validateTags(tags: Seq[LearningPathTag]): Seq[ValidationMessage] = {
    tags.flatMap(tag => {
      noHtmlTextValidator.validate("tags.tag", tag.tag).toList :::
        languageValidator.validate("tags.language", tag.language).toList
    })
  }
}
