/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import com.netaporter.uri.Uri._
import no.ndla.learningpathapi.model.api.{UpdatedLearningPathV2, ValidationMessage}
import no.ndla.learningpathapi.model.domain._
import no.ndla.mapping.License.getLicense

trait LearningPathValidator {
  this: LanguageValidator with TitleValidator =>
  val learningPathValidator: LearningPathValidator

  class LearningPathValidator(titleRequired: Boolean = true, descriptionRequired: Boolean = true) {

    val MISSING_DESCRIPTION = "At least one description is required."

    val INVALID_COVER_PHOTO =
      "The url to the coverPhoto must point to an image in NDLA Image API."

    val noHtmlTextValidator = new TextValidator(allowHtml = false)
    val durationValidator = new DurationValidator

    def validate(newLearningPath: LearningPath, allowUnknownLanguage: Boolean = false): LearningPath = {
      validateLearningPath(newLearningPath, allowUnknownLanguage) match {
        case head :: tail =>
          throw new ValidationException(errors = head :: tail)
        case _ => newLearningPath
      }
    }

    def validate(updateLearningPath: UpdatedLearningPathV2): Unit = {
      languageValidator.validate("language", updateLearningPath.language, allowUnknownLanguage = false) match {
        case None =>
        case Some(validationMessage) =>
          throw new ValidationException(errors = Seq(validationMessage))
      }
    }

    private[validation] def validateLearningPath(newLearningPath: LearningPath,
                                                 allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      titleValidator.validate(newLearningPath.title, allowUnknownLanguage) ++
        validateDescription(newLearningPath.description, allowUnknownLanguage) ++
        validateDuration(newLearningPath.duration).toList ++
        validateTags(newLearningPath.tags, allowUnknownLanguage) ++
        validateCopyright(newLearningPath.copyright)
    }

    private def validateDescription(descriptions: Seq[Description],
                                    allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      (descriptionRequired, descriptions.isEmpty) match {
        case (false, true) => List()
        case (true, true) =>
          List(ValidationMessage("description", MISSING_DESCRIPTION))
        case (_, false) =>
          descriptions.flatMap(description => {
            noHtmlTextValidator
              .validate("description.description", description.description)
              .toList :::
              languageValidator
              .validate("description.language", description.language, allowUnknownLanguage)
              .toList
          })
      }
    }

    private def validateDuration(durationOpt: Option[Int]): Option[ValidationMessage] = {
      durationOpt match {
        case None           => None
        case Some(duration) => durationValidator.validateRequired(durationOpt)
      }
    }

    def validateCoverPhoto(coverPhotoMetaUrl: String): Option[ValidationMessage] = {
      val parsedUrl = parse(coverPhotoMetaUrl)
      val host = parsedUrl.host

      val hostCorrect = host.getOrElse("").endsWith("ndla.no")
      val pathCorrect = parsedUrl.path.startsWith("/image-api/v")

      hostCorrect && pathCorrect match {
        case true => None
        case false =>
          Some(ValidationMessage("coverPhotoMetaUrl", INVALID_COVER_PHOTO))
      }
    }

    private def validateTags(tags: Seq[LearningPathTags], allowUnknownLanguage: Boolean): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags
          .flatMap(noHtmlTextValidator.validate("tags.tags", _))
          .toList :::
          languageValidator
          .validate("tags.language", tagList.language, allowUnknownLanguage)
          .toList
      })
    }

    private def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      val licenseMessage = validateLicense(copyright.license)
      val contributorsMessages = copyright.contributors.flatMap(validateAuthor)

      licenseMessage ++ contributorsMessages
    }

    private def validateLicense(license: String): Seq[ValidationMessage] = {
      getLicense(license) match {
        case None =>
          Seq(new ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    private def validateAuthor(author: Author): Seq[ValidationMessage] = {
      noHtmlTextValidator.validate("author.type", author.`type`).toList ++
        noHtmlTextValidator.validate("author.name", author.name).toList
    }
  }

}
