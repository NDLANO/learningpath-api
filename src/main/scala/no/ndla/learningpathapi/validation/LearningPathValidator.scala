/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import com.netaporter.uri.Uri._
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain._
import no.ndla.mapping.License.getLicense

trait LearningPathValidator {
  this: LanguageValidator with TitleValidator =>
  val learningPathValidator : LearningPathValidator

  class LearningPathValidator(titleRequired: Boolean = true, descriptionRequired: Boolean = true) {

    val MISSING_DESCRIPTION = "At least one description is required."
    val INVALID_COVER_PHOTO = "The url to the coverPhoto must point to an image in NDLA Image API."

    val noHtmlTextValidator = new TextValidator(allowHtml = false)
    val durationValidator = new DurationValidator

    def validate(newLearningPath: LearningPath): LearningPath = {
      validateLearningPath(newLearningPath) match {
        case head :: tail => throw new ValidationException(errors = head :: tail)
        case _ => newLearningPath
      }
    }

    def validateLearningPath(newLearningPath: LearningPath) : Seq[ValidationMessage] = {
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
      coverPhotoMetaUrl.flatMap(url => {
        val parsedUrl = parse(url)
        val host = parsedUrl.host

        val hostCorrect = host.getOrElse("").endsWith("ndla.no")
        val pathCorrect = parsedUrl.path.startsWith("/image-api/v")

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
      getLicense(license) match {
        case None => Seq(new ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    def validateAuthor(author: Author): Seq[ValidationMessage] = {
      noHtmlTextValidator.validate("author.type", author.`type`).toList ++
        noHtmlTextValidator.validate("author.name", author.name).toList
    }
  }

}
