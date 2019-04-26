/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import io.lemonlabs.uri.{QueryString, UrlPath}
import io.lemonlabs.uri.dsl._
import no.ndla.learningpathapi.LearningpathApiProperties.{
  Domain,
  InternalImageApiUrl,
  NdlaFrontendHost,
  NdlaFrontendHostNames
}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api.config.UpdateConfigValue
import no.ndla.learningpathapi.model.api.{LearningPathStatus => _, _}
import no.ndla.learningpathapi.model.domain.Language._
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.validation.{LanguageValidator, LearningPathValidator}
import no.ndla.mapping.License.getLicense
import no.ndla.network.ApplicationUrl

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: ImageApiClientComponent
    with LearningPathRepositoryComponent
    with LanguageValidator
    with LearningPathValidator
    with Clock =>

  val converterService: ConverterService

  class ConverterService {

    def asEmbedUrlV2(embedUrl: api.EmbedUrlV2, language: String): domain.EmbedUrl = {
      domain.EmbedUrl(embedUrl.url, language, EmbedType.valueOfOrError(embedUrl.embedType))
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
        case Some(l) => api.License(l.license.toString, Option(l.description), l.url)
        case None    => api.License(license, Some("Invalid license"), None)
      }

    def asApiAuthor(author: domain.Author): api.Author = {
      api.Author(author.`type`, author.name)
    }

    def asAuthor(user: domain.NdlaUserName): api.Author = {
      val names = Array(user.first_name, user.middle_name, user.last_name)
        .filter(_.isDefined)
        .map(_.get)
      api.Author("Forfatter", names.mkString(" "))
    }

    def asCoverPhoto(imageId: String): Option[CoverPhoto] = {
      imageApiClient
        .imageMetaOnUrl(createUrlToImageApi(imageId))
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

    def asApiLearningpathV2(lp: domain.LearningPath,
                            language: String,
                            fallback: Boolean,
                            userInfo: UserInfo): Try[api.LearningPathV2] = {
      val supportedLanguages = findSupportedLanguages(lp)
      if (languageIsSupported(supportedLanguages, language) || fallback) {

        val searchLanguage = getSearchLanguage(language, supportedLanguages)

        val title = findByLanguageOrBestEffort(lp.title, Some(language))
          .map(asApiTitle)
          .getOrElse(api.Title("", DefaultLanguage))
        val description =
          findByLanguageOrBestEffort(lp.description, Some(language))
            .map(asApiDescription)
            .getOrElse(api.Description("", DefaultLanguage))

        val tags = findByLanguageOrBestEffort(lp.tags, Some(language))
          .map(asApiLearningPathTags)
          .getOrElse(api.LearningPathTags(Seq(), DefaultLanguage))
        val learningSteps = lp.learningsteps
          .flatMap(ls => asApiLearningStepSummaryV2(ls, lp, searchLanguage))
          .toList
          .sortBy(_.seqNo)

        val message = lp.message.filter(_ => lp.canEdit(userInfo)).map(asApiMessage)
        val owner = Some(lp.owner).filter(_ => userInfo.isAdmin)
        Success(
          api.LearningPathV2(
            lp.id.get,
            lp.revision.get,
            lp.isBasedOn,
            title,
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
            lp.canEdit(userInfo),
            supportedLanguages,
            owner,
            message
          ))
      } else
        Failure(NotFoundException(s"Language '$language' is not supported for learningpath ${lp.id.getOrElse(-1)}."))
    }

    private def asApiMessage(message: domain.Message): api.Message =
      api.Message(message.message, message.date)

    private[service] def mergeLanguageFields[A <: LanguageField[String]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.value.isEmpty)
    }

    private def extractImageId(url: String): Option[String] = {
      learningPathValidator.validateCoverPhoto(url) match {
        case Some(err) => throw new ValidationException(errors = Seq(err))
        case _         =>
      }

      val pattern = """.*/images/(\d+)""".r
      pattern.findFirstMatchIn(url.path.toString).map(_.group(1))
    }

    private def mergeLearningPathTags(existing: Seq[domain.LearningPathTags],
                                      updated: Seq[domain.LearningPathTags]): Seq[domain.LearningPathTags] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    private def mergeStatus(existing: LearningPath, user: UserInfo): LearningPathStatus.Value = {
      existing.status match {
        case LearningPathStatus.PUBLISHED if existing.canSetStatus(LearningPathStatus.PUBLISHED, user).isFailure =>
          LearningPathStatus.UNLISTED
        case existingStatus => existingStatus
      }
    }

    def mergeLearningPaths(existing: LearningPath, updated: UpdatedLearningPathV2, userInfo: UserInfo): LearningPath = {
      val status = mergeStatus(existing, userInfo)

      val titles = updated.title match {
        case None => Seq.empty
        case Some(value) =>
          Seq(domain.Title(value, updated.language))
      }

      val descriptions = updated.description match {
        case None => Seq.empty
        case Some(value) =>
          Seq(domain.Description(value, updated.language))
      }

      val tags = updated.tags match {
        case None => Seq.empty
        case Some(value) =>
          Seq(domain.LearningPathTags(value, updated.language))
      }

      val message = existing.message.filterNot(_ => updated.deleteMessage.getOrElse(false))

      existing.copy(
        revision = Some(updated.revision),
        title = mergeLanguageFields(existing.title, titles),
        description = mergeLanguageFields(existing.description, descriptions),
        coverPhotoId = updated.coverPhotoMetaUrl
          .map(extractImageId)
          .getOrElse(existing.coverPhotoId),
        duration =
          if (updated.duration.isDefined)
            updated.duration
          else existing.duration,
        tags = mergeLearningPathTags(existing.tags, tags),
        status = status,
        copyright =
          if (updated.copyright.isDefined)
            converterService.asCopyright(updated.copyright.get)
          else existing.copyright,
        lastUpdated = clock.now(),
        message = message
      )
    }

    def asDomainLearningStep(newLearningStep: NewLearningStepV2, learningPath: LearningPath): LearningStep = {
      val description = newLearningStep.description
        .map(domain.Description(_, newLearningStep.language))
        .toSeq
      val embedUrl = newLearningStep.embedUrl
        .map(converterService.asDomainEmbedUrl(_, newLearningStep.language))
        .toSeq

      val newSeqNo =
        if (learningPath.learningsteps.isEmpty) 0 else learningPath.learningsteps.map(_.seqNo).max + 1

      domain.LearningStep(
        None,
        None,
        None,
        learningPath.id,
        newSeqNo,
        Seq(domain.Title(newLearningStep.title, newLearningStep.language)),
        description,
        embedUrl,
        StepType.valueOfOrError(newLearningStep.`type`),
        newLearningStep.license,
        newLearningStep.showTitle
      )
    }

    def insertLearningSteps(learningPath: LearningPath, steps: Seq[LearningStep], user: UserInfo): LearningPath = {
      steps.foldLeft(learningPath) { (lp, ls) =>
        insertLearningStep(lp, ls, user)
      }
    }

    def insertLearningStep(learningPath: LearningPath, updatedStep: LearningStep, user: UserInfo): LearningPath = {
      val status = mergeStatus(learningPath, user)
      val existingLearningSteps = learningPath.learningsteps.filterNot(_.id == updatedStep.id)
      val steps =
        if (StepStatus.ACTIVE == updatedStep.status) existingLearningSteps :+ updatedStep else existingLearningSteps

      learningPath.copy(learningsteps = steps, status = status, lastUpdated = clock.now())
    }

    def mergeLearningSteps(existing: LearningStep, updated: UpdatedLearningStepV2): LearningStep = {
      val titles = updated.title match {
        case None => existing.title
        case Some(value) =>
          converterService.mergeLanguageFields(existing.title, Seq(domain.Title(value, updated.language)))
      }

      val descriptions = updated.description match {
        case None => existing.description
        case Some(value) =>
          converterService.mergeLanguageFields(existing.description, Seq(domain.Description(value, updated.language)))
      }

      val embedUrls = updated.embedUrl match {
        case None => existing.embedUrl
        case Some(value) =>
          converterService.mergeLanguageFields(existing.embedUrl,
                                               Seq(converterService.asDomainEmbedUrl(value, updated.language)))
      }

      existing.copy(
        revision = Some(updated.revision),
        title = titles,
        description = descriptions,
        embedUrl = embedUrls,
        showTitle = updated.showTitle.getOrElse(existing.showTitle),
        `type` = updated.`type`
          .map(domain.StepType.valueOfOrError)
          .getOrElse(existing.`type`),
        license = updated.license
      )
    }

    def newFromExistingLearningPath(existing: LearningPath,
                                    newLearningPath: NewCopyLearningPathV2,
                                    user: UserInfo): LearningPath = {
      val oldTitle = Seq(domain.Title(newLearningPath.title, newLearningPath.language))

      val oldDescription = newLearningPath.description match {
        case None => Seq.empty
        case Some(value) =>
          Seq(domain.Description(value, newLearningPath.language))
      }

      val oldTags = newLearningPath.tags match {
        case None => Seq.empty
        case Some(value) =>
          Seq(domain.LearningPathTags(value, newLearningPath.language))
      }

      val title = converterService.mergeLanguageFields(existing.title, oldTitle)
      val description = converterService.mergeLanguageFields(existing.description, oldDescription)
      val tags = converterService.mergeLearningPathTags(existing.tags, oldTags)
      val coverPhotoId = newLearningPath.coverPhotoMetaUrl
        .map(converterService.extractImageId)
        .getOrElse(existing.coverPhotoId)
      val duration =
        if (newLearningPath.duration.nonEmpty) newLearningPath.duration
        else existing.duration
      val copyright = newLearningPath.copyright
        .map(converterService.asCopyright)
        .getOrElse(existing.copyright)

      existing.copy(
        id = None,
        revision = None,
        externalId = None,
        isBasedOn = if (existing.isPrivate) None else existing.id,
        title = title,
        description = description,
        status = LearningPathStatus.PRIVATE,
        verificationStatus = LearningPathVerificationStatus.EXTERNAL,
        lastUpdated = clock.now(),
        owner = user.userId,
        copyright = copyright,
        learningsteps =
          existing.learningsteps.map(_.copy(id = None, revision = None, externalId = None, learningPathId = None)),
        tags = tags,
        coverPhotoId = coverPhotoId,
        duration = duration
      )
    }

    def newLearningPath(newLearningPath: NewLearningPathV2, user: UserInfo): LearningPath = {
      val domainTags =
        if (newLearningPath.tags.isEmpty) Seq.empty
        else
          Seq(domain.LearningPathTags(newLearningPath.tags, newLearningPath.language))

      domain.LearningPath(
        None,
        None,
        None,
        None,
        Seq(domain.Title(newLearningPath.title, newLearningPath.language)),
        Seq(domain.Description(newLearningPath.description, newLearningPath.language)),
        newLearningPath.coverPhotoMetaUrl.flatMap(converterService.extractImageId),
        newLearningPath.duration,
        domain.LearningPathStatus.PRIVATE,
        LearningPathVerificationStatus.EXTERNAL,
        clock.now(),
        domainTags,
        user.userId,
        converterService.asCopyright(newLearningPath.copyright),
        List()
      )
    }

    def getApiIntroduction(learningSteps: Seq[domain.LearningStep]): Seq[api.Introduction] = {
      learningSteps
        .find(_.`type` == domain.StepType.INTRODUCTION)
        .toList
        .flatMap(x => x.description)
        .map(x => api.Introduction(x.description, x.language))
    }

    def languageIsNotSupported(supportedLanguages: Seq[String], language: String): Boolean = {
      supportedLanguages.isEmpty || (!supportedLanguages.contains(language) && language != AllLanguages)
    }

    def asApiLearningpathSummaryV2(learningpath: domain.LearningPath,
                                   user: UserInfo = UserInfo.get): Try[api.LearningPathSummaryV2] = {
      val supportedLanguages = findSupportedLanguages(learningpath)

      val title = findByLanguageOrBestEffort(learningpath.title, Some(Language.AllLanguages))
        .map(asApiTitle)
        .getOrElse(api.Title("", DefaultLanguage))
      val description = findByLanguageOrBestEffort(learningpath.description, Some(Language.AllLanguages))
        .map(asApiDescription)
        .getOrElse(api.Description("", DefaultLanguage))
      val tags = findByLanguageOrBestEffort(learningpath.tags, Some(Language.AllLanguages))
        .map(asApiLearningPathTags)
        .getOrElse(api.LearningPathTags(Seq(), DefaultLanguage))
      val introduction =
        findByLanguageOrBestEffort(getApiIntroduction(learningpath.learningsteps), Some(Language.AllLanguages))
          .getOrElse(api.Introduction("", DefaultLanguage))

      val message = learningpath.message.filter(_ => learningpath.canEdit(user)).map(_.message)

      Success(
        api.LearningPathSummaryV2(
          learningpath.id.get,
          revision = learningpath.revision,
          title,
          description,
          introduction,
          createUrlToLearningPath(learningpath),
          learningpath.coverPhotoId.flatMap(asCoverPhoto).map(_.url),
          learningpath.duration,
          learningpath.status.toString,
          learningpath.lastUpdated,
          tags,
          asApiCopyright(learningpath.copyright),
          supportedLanguages,
          learningpath.isBasedOn,
          message
        )
      )
    }

    def languageIsSupported(supportedLangs: Seq[String], language: String): Boolean = {
      val isLanguageNeutral = supportedLangs.contains(UnknownLanguage) && supportedLangs.length == 1

      supportedLangs.contains(language) || language == AllLanguages || isLanguageNeutral
    }

    def asApiLearningStepV2(ls: domain.LearningStep,
                            lp: domain.LearningPath,
                            language: String,
                            fallback: Boolean,
                            user: UserInfo): Try[api.LearningStepV2] = {
      val supportedLanguages = findSupportedLanguages(ls)

      if (languageIsSupported(supportedLanguages, language) || fallback) {
        val title = findByLanguageOrBestEffort(ls.title, Some(language))
          .map(asApiTitle)
          .getOrElse(api.Title("", DefaultLanguage))
        val description =
          findByLanguageOrBestEffort(ls.description, Some(language))
            .map(asApiDescription)
        val embedUrl = findByLanguageOrBestEffort(ls.embedUrl, Some(language))
          .map(asApiEmbedUrlV2)
          .map(createEmbedUrl)

        Success(
          api.LearningStepV2(
            ls.id.get,
            ls.revision.get,
            ls.seqNo,
            title,
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
      } else { Failure(NotFoundException("Learningpath with id $id and language $language not found")) }
    }

    def asApiLearningStepSummaryV2(ls: domain.LearningStep,
                                   lp: domain.LearningPath,
                                   language: String): Option[api.LearningStepSummaryV2] = {
      findByLanguageOrBestEffort(ls.title, Some(language)).map(
        title =>
          api.LearningStepSummaryV2(
            ls.id.get,
            ls.seqNo,
            asApiTitle(title),
            ls.`type`.toString,
            createUrlToLearningStep(ls, lp)
        ))
    }

    def asLearningStepContainerSummary(status: StepStatus.Value,
                                       learningPath: domain.LearningPath,
                                       language: String,
                                       fallback: Boolean): Try[api.LearningStepContainerSummary] = {
      val learningSteps = learningPathRepository
        .learningStepsFor(learningPath.id.get)
        .filter(_.status == status)
      val supportedLanguages =
        learningSteps.flatMap(_.title).map(_.language).distinct

      if ((languageIsSupported(supportedLanguages, language) || fallback) && learningSteps.nonEmpty) {
        val searchLanguage =
          if (supportedLanguages.contains(language) || language == AllLanguages)
            getSearchLanguage(language, supportedLanguages)
          else language

        Success(
          api.LearningStepContainerSummary(
            searchLanguage,
            learningSteps
              .flatMap(ls =>
                converterService
                  .asApiLearningStepSummaryV2(ls, learningPath, searchLanguage))
              .sortBy(_.seqNo),
            supportedLanguages
          ))
      } else
        Failure(
          NotFoundException(s"Learningpath with id ${learningPath.id.getOrElse(-1)} and language $language not found"))
    }

    def asApiLearningPathTagsSummary(allTags: List[api.LearningPathTags],
                                     language: String,
                                     fallback: Boolean): Option[api.LearningPathTagsSummary] = {
      val supportedLanguages = allTags.map(_.language).distinct

      if (languageIsSupported(supportedLanguages, language) || fallback) {
        val searchLanguage =
          Language.getSearchLanguage(language, supportedLanguages)
        val tags = allTags
          .filter(_.language == searchLanguage)
          .flatMap(_.tags)

        Some(
          api.LearningPathTagsSummary(
            searchLanguage,
            supportedLanguages,
            tags
          ))
      } else
        None
    }

    def asApiTitle(title: domain.Title): api.Title = {
      api.Title(title.title, title.language)
    }

    def asApiDescription(description: domain.Description): api.Description = {
      api.Description(description.description, description.language)
    }

    def asApiEmbedUrlV2(embedUrl: domain.EmbedUrl): api.EmbedUrlV2 = {
      api.EmbedUrlV2(embedUrl.url, embedUrl.embedType.toString)
    }

    def asDomainEmbedUrl(embedUrl: api.EmbedUrlV2, language: String): domain.EmbedUrl = {
      val url = embedUrl.url.hostOption match {
        case Some(host) if NdlaFrontendHostNames.contains(host.toString) =>
          val pathAndQueryParams: String = embedUrl.url.path.toString.withQueryString(embedUrl.url.query)
          pathAndQueryParams
        case _ => embedUrl.url
      }

      domain.EmbedUrl(url, language, EmbedType.valueOfOrError(embedUrl.embedType))
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

    def createUrlToLearningPath(lp: api.LearningPathV2): String = {
      s"${ApplicationUrl.get}${lp.id}"
    }

    def createUrlToImageApi(imageId: String): String = {
      s"http://$InternalImageApiUrl/$imageId"
    }

    def createEmbedUrl(embedUrlOrPath: EmbedUrlV2): EmbedUrlV2 = {
      embedUrlOrPath.url.hostOption match {
        case Some(_) => embedUrlOrPath
        case None =>
          embedUrlOrPath.copy(url = s"https://$NdlaFrontendHost${embedUrlOrPath.url}")
      }
    }

    def asApiConfig(configValue: ConfigMeta): api.config.ConfigMeta = {
      api.config.ConfigMeta(
        configValue.key.toString,
        configValue.value,
        configValue.updatedAt,
        configValue.updatedBy
      )
    }
  }
}
