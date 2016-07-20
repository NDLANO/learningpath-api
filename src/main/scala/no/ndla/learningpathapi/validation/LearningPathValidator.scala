package no.ndla.learningpathapi.validation

import com.netaporter.uri.Uri._
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain._
import no.ndla.mapping.LicenseMapping.getLicenseDefinition


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
      validateTags(newLearningPath.tags) ++
      validateCopyright(newLearningPath.copyright)
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
    val possibleImageApiDomain = parse(LearningpathApiProperties.Domain).host

    coverPhotoMetaUrl.flatMap(url => {
      val parsedUrl = parse(url)
      val host = parsedUrl.host

      val hostCorrect = possibleImageApiDomain == host
      val pathCorrect = parsedUrl.path.startsWith("/images")

      hostCorrect && pathCorrect match {
        case true => None
        case false => Some(ValidationMessage("coverPhotoMetaUrl", INVALID_COVER_PHOTO))
      }
    })
  }

  def validateTags(tags: Seq[LearningPathTags]): Seq[ValidationMessage] = {
    tags.flatMap(tagList => {
      tagList.tags.flatMap(noHtmlTextValidator.validate("tags.tags", _)).toList :::
      languageValidator.validate("tags.language", tagList.language).toList
    })
  }

  def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
    val licenseMessage = validateLicense(copyright.license)
    val contributorsMessages = copyright.contributors.flatMap(validateAuthor)

    licenseMessage ++ contributorsMessages
  }

  def validateLicense(license: String): Seq[ValidationMessage] = {
    getLicenseDefinition(license) match {
      case None => Seq(new ValidationMessage("license.license", s"${license} is not a valid license"))
      case _ => Seq()
    }
  }

  def validateAuthor(author: Author): Seq[ValidationMessage] = {
    noHtmlTextValidator.validate("author.type", author.`type`).toList ++
      noHtmlTextValidator.validate("author.name", author.name).toList
  }
}
