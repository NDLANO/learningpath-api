/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.api.{CoverPhoto, LearningStepSummaryV2}
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.LearningpathApiProperties.{Domain, InternalImageApiUrl}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.domain.Language._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.network.ApplicationUrl
import no.ndla.mapping.License.getLicense
import com.netaporter.uri.dsl._

trait ConverterServiceComponent {
  this: ImageApiClientComponent with LearningPathRepositoryComponent =>

  val converterService: ConverterService

  class ConverterService {
    def asEmbedUrl(embedUrl: api.EmbedUrl): domain.EmbedUrl = {
      domain.EmbedUrl(embedUrl.url, embedUrl.language, EmbedType.valueOfOrError(embedUrl.embedType))
    }

    def asDescription(description: api.Description): domain.Description = {
      domain.Description(description.description, description.language)
    }

    def asTitle(title: api.Title): domain.Title = {
      domain.Title(title.title, title.language)
    }

    def asLearningPathTags(tags: api.LearningPathTags): domain.LearningPathTags = {
      domain.LearningPathTags(tags.tags, tags.language)
    }

    def asApiLearningPathTags(tags: domain.LearningPathTags): api.LearningPathTags = {
      api.LearningPathTags(tags.tags, tags.language)
    }

    def asApiCopyright(copyright: domain.Copyright): api.Copyright = {
      api.Copyright(asApiLicense(copyright.license), copyright.contributors.map(asApiAuthor))
    }

    def asApiLicense(license: String): api.License =
      getLicense(license) match {
        case Some(l) => api.License(l.license, Option(l.description), l.url)
        case None => api.License(license, Some("Invalid license"), None)
      }

    def asApiAuthor(author: domain.Author): api.Author = {
      api.Author(author.`type`, author.name)
    }

    def asAuthor(user: domain.NdlaUserName): api.Author = {
      val names = Array(user.first_name, user.middle_name, user.last_name).filter(_.isDefined).map(_.get)
      api.Author("Forfatter", names.mkString(" "))
    }

    def asCoverPhoto(imageId: String): Option[CoverPhoto] = {
      imageApiClient.imageMetaOnUrl(createUrlToImageApi(imageId))
        .map(imageMeta => {
          val imageUrl = s"$Domain${imageMeta.imageUrl.path}"
          val metaUrl = s"$Domain${imageMeta.metaUrl.path}"
          api.CoverPhoto(imageUrl, metaUrl)
        })
    }

    def asCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(copyright.license.license, copyright.contributors.map(asAuthor))
    }

    def asAuthor(author: api.Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }

    def asApiLearningpath(lp: domain.LearningPath, user: Option[String]): api.LearningPath = {
      api.LearningPath(lp.id.get,
        lp.revision.get,
        lp.isBasedOn,
        lp.title.map(asApiTitle),
        lp.description.map(asApiDescription),
        createUrlToLearningPath(lp),
        lp.learningsteps.map(ls => asApiLearningStepSummary(ls, lp)).toList.sortBy(_.seqNo),
        createUrlToLearningSteps(lp),
        lp.coverPhotoId.flatMap(asCoverPhoto),
        lp.duration,
        lp.status.toString,
        lp.verificationStatus.toString,
        lp.lastUpdated,
        lp.tags.map(asApiLearningPathTags),
        asApiCopyright(lp.copyright),
        lp.canEdit(user))
    }

    def asApiLearningpathV2(lp: domain.LearningPath, language: String, user: Option[String]): Option[api.LearningPathV2] = {
      val supportedLanguages = findSupportedLanguages(lp)
      if (supportedLanguages.isEmpty) {
        return None
      }

      val searchLanguage = getSearchLanguage(language, supportedLanguages)

      val title =         findValueByLanguage(lp.title, searchLanguage).getOrElse("")
      val description =   findValueByLanguage(lp.description, searchLanguage).getOrElse("")
      val tags =          findValueByLanguage(lp.tags, searchLanguage).getOrElse(Seq.empty[String])
      val learningSteps = {
        val lS = lp.learningsteps.flatMap(ls => asApiLearningStepSummaryV2(ls, lp, searchLanguage)).toList.sortBy(_.seqNo)
        if (lS.nonEmpty) lS
        else Seq.empty[LearningStepSummaryV2]
      }

      Some(api.LearningPathV2(
        lp.id.get,
        lp.revision.get,
        lp.isBasedOn,
        title,
        searchLanguage,
        description,
        createUrlToLearningPath(lp),
        learningSteps,
        createUrlToLearningSteps(lp),
        lp.coverPhotoId.flatMap(asCoverPhoto),
        lp.duration,
        lp.status.toString,
        lp.verificationStatus.toString,
        lp.lastUpdated,
        tags,
        asApiCopyright(lp.copyright),
        lp.canEdit(user),
        supportedLanguages
      ))
    }

    def asApiIntroduction(introStepOpt: Option[domain.LearningStep]): Seq[api.Introduction] = {
      introStepOpt match {
        case None => List()
        case Some(introStep) => introStep.description.map(desc => api.Introduction(desc.description, desc.language))
      }
    }

    def asApiLearningpathSummary(learningpath: domain.LearningPath): api.LearningPathSummary = {
      api.LearningPathSummary(learningpath.id.get,
        learningpath.title.map(asApiTitle),
        learningpath.description.map(asApiDescription),
        asApiIntroduction(learningpath.learningsteps.find(_.`type` == domain.StepType.INTRODUCTION)),
        createUrlToLearningPath(learningpath),
        learningpath.coverPhotoId.flatMap(asCoverPhoto).map(_.url),
        learningpath.duration,
        learningpath.status.toString,
        learningpath.lastUpdated,
        learningpath.tags.map(asApiLearningPathTags),
        asApiCopyright(learningpath.copyright),
        learningpath.isBasedOn)
    }

    def languageIsNotSupported(supportedLanguages: Seq[String], language: String): Boolean = {
      supportedLanguages.isEmpty || (!supportedLanguages.contains(language) && language != AllLanguages)
    }

    def asApiLearningpathSummaryV2(learningpath: domain.LearningPath, language: String): Option[api.LearningPathSummaryV2] = {
      val supportedLanguages = findSupportedLanguages(learningpath)
      if (languageIsNotSupported(supportedLanguages, language)) {
        return None
      }

      val searchLanguage = getSearchLanguage(language, supportedLanguages)

      val title =          findValueByLanguage(learningpath.title, searchLanguage).getOrElse("")
      val description =    findValueByLanguage(learningpath.description, searchLanguage).getOrElse("")
      val tags =           findValueByLanguage(learningpath.tags, searchLanguage).getOrElse(Seq.empty[String])

      val introduction = learningpath.learningsteps
        .flatMap(_.description)
        .find(_.language.getOrElse("") == searchLanguage)
        .getOrElse(Description("", None))
        .description

      Some(
        api.LearningPathSummaryV2(
          learningpath.id.get,
          title,
          description,
          introduction,
          createUrlToLearningPath(learningpath),
          learningpath.coverPhotoId.flatMap(asCoverPhoto).map(_.url),
          learningpath.duration,
          learningpath.status.toString,
          learningpath.lastUpdated.toString,
          tags,
          asApiCopyright(learningpath.copyright),
          supportedLanguages,
          learningpath.isBasedOn
        )
      )
    }

    def asApiLearningStep(ls: domain.LearningStep, lp: domain.LearningPath, user: Option[String]): api.LearningStep = {
      api.LearningStep(
        ls.id.get,
        ls.revision.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.description.map(asApiDescription),
        ls.embedUrl.map(asApiEmbedUrl),
        ls.showTitle,
        ls.`type`.toString,
        ls.license.map(asApiLicense),
        createUrlToLearningStep(ls, lp),
        lp.canEdit(user),
        ls.status.toString)
    }

    def asApiLearningStepV2(ls: domain.LearningStep, lp: domain.LearningPath, language: String, user: Option[String]): Option[api.LearningStepV2] = {
      val supportedLanguages = findSupportedLanguages(ls)
      if (supportedLanguages.isEmpty) {
        return None
      }

      val searchLanguage = getSearchLanguage(language, supportedLanguages)

      val title =       findValueByLanguage(ls.title, searchLanguage).getOrElse("")
      val description = findValueByLanguage(ls.description, searchLanguage).getOrElse("")
      val embedUrl =    findByLanguage(ls.embedUrl, searchLanguage)
                          .asInstanceOf[Option[domain.EmbedUrl]]
                          .map(asApiEmbedUrlV2)
                          .getOrElse(api.EmbedUrlV2("", ""))

      Some(api.LearningStepV2(
        ls.id.get,
        ls.revision.get,
        ls.seqNo,
        title,
        searchLanguage,
        description,
        embedUrl,
        ls.showTitle,
        ls.`type`.toString,
        ls.license.map(asApiLicense),
        createUrlToLearningStep(ls, lp),
        lp.canEdit(user),
        ls.status.toString,
        supportedLanguages
      ))
    }

    def asApiLearningStepSummary(ls: domain.LearningStep, lp: domain.LearningPath): api.LearningStepSummary = {
      api.LearningStepSummary(
        ls.id.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.`type`.toString,
        createUrlToLearningStep(ls, lp)
      )
    }

    def asApiLearningStepSummaryV2(ls: domain.LearningStep, lp: domain.LearningPath, language: String): Option[api.LearningStepSummaryV2] = {
      findValueByLanguage(ls.title, language).map(title =>
        api.LearningStepSummaryV2(
          ls.id.get,
          ls.seqNo,
          title,
          ls.`type`.toString,
          createUrlToLearningStep(ls, lp)
        )
      )
    }

    def asLearningStepContainerSummary(status: StepStatus.Value, learningPath: domain.LearningPath, language: String): api.LearningStepContainerSummary = {
      val learningSteps = learningPathRepository.learningStepsFor(learningPath.id.get).filter(_.status == status)
      val supportedLanguages = learningSteps.flatMap(_.title).flatMap(_.language)

      val searchLanguage =
        if (supportedLanguages.contains(language) || language == AllLanguages)
          getSearchLanguage(language, supportedLanguages)
        else language

      api.LearningStepContainerSummary(
        searchLanguage,
        learningSteps.flatMap(ls => converterService.asApiLearningStepSummaryV2(ls, learningPath, searchLanguage)).sortBy(_.seqNo),
        supportedLanguages
      )
    }

    def asApiLearningPathTagsSummary(allTags: List[api.LearningPathTags], language: String): Option[api.LearningPathTagsSummary] = {
      val supportedLanguages = allTags.flatMap(_.language).distinct

      if (language != Language.AllLanguages && !supportedLanguages.contains(language)) {
        return None
      }

      val searchLanguage = Language.getSearchLanguage(language, supportedLanguages)
      val tags = allTags
        .filter(_.language.getOrElse("") == searchLanguage)
        .flatMap(_.tags)

      Some(api.LearningPathTagsSummary(
        searchLanguage,
        supportedLanguages,
        tags
      ))

    }

    def asApiTitle(title: domain.Title): api.Title = {
      api.Title(title.title, title.language)
    }

    def asApiDescription(description: domain.Description): api.Description = {
      api.Description(description.description, description.language)
    }

    def asApiEmbedUrl(embedUrl: domain.EmbedUrl): api.EmbedUrl = {
      api.EmbedUrl(embedUrl.url, embedUrl.language, embedUrl.embedType.toString)
    }

    def asApiEmbedUrlV2(embedUrl: domain.EmbedUrl): api.EmbedUrlV2 = {
      api.EmbedUrlV2(embedUrl.url, embedUrl.embedType.toString)
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

    def createUrlToLearningPath(lp: api.LearningPath): String = {
      s"${ApplicationUrl.get}${lp.id}"
    }

    def createUrlToImageApi(imageId: String): String = {
      s"http://$InternalImageApiUrl/$imageId"
    }
  }
}
