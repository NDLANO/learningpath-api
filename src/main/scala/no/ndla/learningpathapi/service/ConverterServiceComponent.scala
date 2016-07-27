package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain.{EmbedUrl, LearningStep, NdlaUserName, StepType}
import no.ndla.learningpathapi.model._
import no.ndla.mapping.LicenseMapping.getLicenseDefinition
import no.ndla.network.ApplicationUrl

trait ConverterServiceComponent {
  this: AuthClientComponent with ImageApiClientComponent =>
  val converterService: ConverterService

  class ConverterService {
    def asEmbedUrl(embedContent: EmbedContent): EmbedUrl = {
      domain.EmbedUrl(embedContent.url, embedContent.language)
    }

    def asDescription(description: Description): domain.Description = {
      domain.Description(description.description, description.language)
    }

    def asTitle(title: Title): domain.Title = {
      domain.Title(title.title, title.language)
    }

    def asLearningPathTags(tags: LearningPathTags): domain.LearningPathTags = {
      domain.LearningPathTags(tags.tags, tags.language)
    }

    def asApiLearningPathTags(tags: domain.LearningPathTags): LearningPathTags = {
      LearningPathTags(tags.tags, tags.language)
    }

    def asApiCopyright(copyright: domain.Copyright): api.Copyright = {
      api.Copyright(asApiLicense(copyright.license), copyright.contributors.map(asApiAuthor))
    }

    def asApiLicense(license: String): api.License =
      getLicenseDefinition(license) match {
        case Some(l) => api.License(l.license, Some(l.description), l.url)
        case None => api.License(license, Some("Invalid license"), None)
      }

    def asApiAuthor(author: domain.Author): api.Author = {
      api.Author(author.`type`, author.name)
    }

    def asAuthor(user: NdlaUserName): Author = {
      val names = Array(user.first_name, user.middle_name, user.last_name).filter(_.isDefined).map(_.get)
      Author("Forfatter", names.mkString(" "))
    }

    def asCoverPhoto(metaUrl: String, imageMeta: Option[ImageMetaInformation]): Option[CoverPhoto] = {
      imageMeta.flatMap(_.images.full).map(full => CoverPhoto(full.url, metaUrl))
    }

    def asCopyright(copyright: Copyright): domain.Copyright = {
      domain.Copyright(copyright.license.license, copyright.contributors.map(asAuthor))
    }

    def asAuthor(author: Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }

    def asApiLearningpath(lp: domain.LearningPath, user: Option[String]): LearningPath = {
      api.LearningPath(lp.id.get,
        lp.revision.get,
        lp.isBasedOn,
        lp.title.map(asApiTitle),
        lp.description.map(asApiDescription),
        createUrlToLearningPath(lp),
        lp.learningsteps.map(ls => asApiLearningStepSummary(ls, lp)).toList.sortBy(_.seqNo),
        createUrlToLearningSteps(lp),
        lp.coverPhotoMetaUrl.flatMap(metaUrl => asCoverPhoto(metaUrl, imageApiClient.imageMetaOnUrl(metaUrl))),
        lp.duration,
        lp.status.toString,
        lp.verificationStatus.toString,
        lp.lastUpdated,
        lp.tags.map(asApiLearningPathTags),
        asAuthor(authClient.getUserName(lp.owner)),
        asApiCopyright(lp.copyright),
        lp.canEdit(user))
    }

    def asApiIntroduction(introStepOpt: Option[LearningStep]): Seq[Introduction] = {
      introStepOpt match {
        case None => List()
        case Some(introStep) => introStep.description.map(desc => Introduction(desc.description, desc.language))
      }
    }

    def asApiLearningpathSummary(learningpath: domain.LearningPath): LearningPathSummary = {
      api.LearningPathSummary(learningpath.id.get,
        learningpath.title.map(asApiTitle),
        learningpath.description.map(asApiDescription),
        asApiIntroduction(learningpath.learningsteps.find(_.`type` == StepType.INTRODUCTION)),
        createUrlToLearningPath(learningpath),
        learningpath.coverPhotoMetaUrl.flatMap(metaUrl => asCoverPhoto(metaUrl, imageApiClient.imageMetaOnUrl(metaUrl)).map(_.url)),
        learningpath.duration,
        learningpath.status.toString,
        learningpath.lastUpdated,
        learningpath.tags.map(asApiLearningPathTags),
        asAuthor(authClient.getUserName(learningpath.owner)))
    }

    def asApiLearningStep(ls: domain.LearningStep, lp: domain.LearningPath, user: Option[String]): api.LearningStep = {
      api.LearningStep(
        ls.id.get,
        ls.revision.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.description.map(asApiDescription),
        ls.embedUrl.map(e => asApiEmbedContent(e)),
        ls.showTitle,
        ls.`type`.toString,
        ls.license.map(asApiLicense),
        createUrlToLearningStep(ls, lp),
        lp.canEdit(user),
        ls.status.toString)
    }

    def asApiLearningStepSummary(ls: domain.LearningStep, lp: domain.LearningPath): LearningStepSummary = {
      LearningStepSummary(
        ls.id.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.`type`.toString,
        createUrlToLearningStep(ls, lp)
      )
    }

    def asApiTitle(title: domain.Title): Title = {
      api.Title(title.title, title.language)
    }

    def asApiDescription(description: domain.Description): Description = {
      api.Description(description.description, description.language)
    }

    def asApiEmbedContent(embedUrl: EmbedUrl): EmbedContent = {
      api.EmbedContent(
        embedUrl.url,
        embedUrl.language)
    }

    def createUrlToLearningStep(ls: domain.LearningStep, lp: domain.LearningPath): String = {
      s"${createUrlToLearningSteps(lp)}/${ls.id.get}"
    }

    def createUrlToLearningSteps(lp: domain.LearningPath): String = {
      s"${createUrlToLearningPath(lp)}/learningsteps"
    }

    def createUrlToLearningPath(lp: domain.LearningPath): String = {
      s"${ApplicationUrl.get}${lp.id.get}"
    }

    def createUrlToLearningPath(lp: LearningPath): String = {
      s"${ApplicationUrl.get}${lp.id}"
    }
  }
}
