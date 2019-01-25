/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import io.lemonlabs.uri.dsl._
import no.ndla.learningpathapi.integration.{KeywordsServiceComponent, _}
import no.ndla.learningpathapi.model.api.{ImportReport, ImportStatus}
import no.ndla.learningpathapi.model.domain.Language.languageOrUnknown
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.mapping.License._
import no.ndla.mapping.LicenseDefinition

import scala.util.{Failure, Success, Try}

trait ImportService {
  this: LearningPathRepositoryComponent
    with KeywordsServiceComponent
    with ImageApiClientComponent
    with MigrationApiClient
    with SearchIndexService
    with ConverterService
    with ArticleImportClient
    with TaxonomyApiClient =>
  val importService: ImportService

  class ImportService {
    private val NdlaDomains = Seq("ndla.no", "red.ndla.no")

    def doImport(nodeId: String, clientId: String, importId: String): Try[ImportReport] = {
      val metaData = migrationApiClient.getLearningPath(nodeId)
      val packageId = metaData.map(mp => s"${mp.mainPackage.packageId}").getOrElse(nodeId)

      learningPathRepository.idAndimportIdOfLearningpath(packageId) match {
        case Some((lpId, Some(existingImportId))) if existingImportId == importId =>
          val msg =
            s"Learningpath with node id $nodeId has already imported (same import id) and will not be re-imported"
          logger.info(msg)
          Success(ImportReport(nodeId, ImportStatus.OK, Seq(msg), Some(lpId)))
        case _ =>
          val learningpathSummary = for {
            data <- metaData
            converted <- convert(nodeId, data, clientId, importId)
            persisted <- upload(converted, importId)
            _ <- searchIndexService.indexDocument(persisted)
            summary <- converterService.asApiLearningpathSummaryV2(persisted)
          } yield summary

          learningpathSummary match {
            case Success(ls) => Success(ImportReport(nodeId, ImportStatus.OK, Seq.empty, Some(ls.id)))
            case Failure(ex) =>
              logger.error(s"Failed to import learningpath with node id $nodeId (${ex.getMessage}).", ex)
              metaData.toOption
                .flatMap(data => learningPathRepository.withExternalId(data.mainPackage.packageId.toString))
                .map(existingLP => {
                  val pathId = existingLP.id
                  val stepIds = existingLP.learningsteps.flatMap(_.id)
                  logger.info(
                    s"Failed to import learningpath with node id $nodeId. Deleting previously imported learningpath with id $pathId and steps ${stepIds
                      .mkString(",")}")
                  stepIds.foreach(id => learningPathRepository.deleteStep(id)) // delete learningsteps
                  learningPathRepository.deletePath(pathId.get) // delete learningpath
                })
              Failure(ex)
          }
      }
    }

    private[service] def convert(nodeId: String,
                                 mainImport: MainPackageImport,
                                 clientId: String,
                                 importId: String): Try[LearningPath] = {
      val embedUrls = (mainImport.translations :+ mainImport.mainPackage)
        .flatMap(_.steps)
        .flatMap(_.embedUrl)
        .filter(_.nonEmpty)
      val (articleImports, failedImports) =
        importArticles(embedUrls, importId).partition {
          case (_, importStatus) => importStatus.isSuccess
        }

      if (failedImports.nonEmpty) {
        val messages = failedImports.map(a => a._2.failed.get.getMessage)
        Failure(ImportReport(nodeId, ImportStatus.ERROR, messages, None))
      } else {
        val embedUrlMap = articleImports.map {
          case (oldUrl, taxonomyResource) if NdlaDomains.contains(oldUrl.hostOption.map(_.toString).getOrElse("")) =>
            oldUrl -> Option(taxonomyResource.get.path.toString)
          case (oldUrl, _) => oldUrl -> None
        }.toMap

        val coverPhoto =
          mainImport.mainPackage.imageNid.flatMap(importCoverPhoto)
        val learningPathWithOldEmbedUrls =
          asLearningPath(mainImport, coverPhoto, clientId)

        val steps = learningPathWithOldEmbedUrls.learningsteps.map {
          case l: LearningStep if l.embedUrl.nonEmpty =>
            val embedUrls = l.embedUrl.map(embed => {
              val path =
                embedUrlMap.get(embed.url).flatten.map(p => s"/${embed.language}$p")
              embed.copy(url = path.getOrElse(embed.url)) // Fall back on old url if not ndla-url
            })
            l.copy(embedUrl = embedUrls)
          case l => l
        }

        Success(learningPathWithOldEmbedUrls.copy(learningsteps = steps))
      }
    }

    private def upload(learningpath: LearningPath, importId: String): Try[LearningPath] = {
      learningpath.externalId.flatMap(learningPathRepository.withExternalId) match {
        case None => Try(learningPathRepository.insertWithImportId(learningpath, importId))
        case Some(existingLearningPath) =>
          val updatedLp = Try(
            learningPathRepository.updateWithImportId(asLearningPath(existingLearningPath, learningpath), importId))
          learningpath.learningsteps.foreach(learningStep => {
            learningPathRepository.learningStepWithExternalIdAndForLearningPath(learningStep.externalId,
                                                                                existingLearningPath.id) match {
              case None =>
                learningPathRepository.insertLearningStep(learningStep.copy(learningPathId = existingLearningPath.id))
              case Some(existingLearningStep) =>
                learningPathRepository.updateLearningStep(asLearningStep(existingLearningStep, learningStep))
            }
          })
          updatedLp
      }
    }

    private def importCoverPhoto(imageNid: Int): Option[ImageMetaInformation] = {
      imageApiClient
        .imageMetaWithExternalId(imageNid.toString)
        .orElse(imageApiClient.importImage(imageNid.toString))
    }

    private def importArticles(embedUrls: Seq[String], importId: String): Seq[(String, Try[String])] = {
      def getNodeIdFromUrl = (url: String) => url.trim().path.parts.filter(_.nonEmpty).lastOption
      val mainNodeIds = embedUrls
        .map(url => {
          if (NdlaDomains.contains(url.hostOption.map(_.toString).getOrElse(""))) {
            url -> getNodeIdFromUrl(url).map(migrationApiClient.getAllNodeIds)
          } else {
            url -> None
          }
        })
        .filter(_._2.isDefined)
        .toMap

      val nodeIdsToTaxonomyResourceMap = mainNodeIds.values
        .map(_.getOrElse(Set.empty))
        .map(nids => nids -> importAndUpdateTaxonomy(nids, importId))
        .toMap

      embedUrls.map(embedUrl => {
        val nodeIdsForArticle =
          mainNodeIds.get(embedUrl).flatten.getOrElse(Set.empty)
        val taxonomyResourceForArticle =
          nodeIdsToTaxonomyResourceMap.getOrElse(nodeIdsForArticle, Success(embedUrl))
        embedUrl -> taxonomyResourceForArticle
      })
    }

    private def importAndUpdateTaxonomy(nodeWithTranslations: Set[ArticleMigrationContent],
                                        importId: String): Try[String] = {
      val articleOpt = nodeWithTranslations
        .find(_.isMainNode)
        .map(mainNodeId => articleImportClient.importArticle(mainNodeId.nid, importId))

      (nodeWithTranslations
         .map(c => taxononyApiClient.getResource(c.nid))
         .find(_.isSuccess),
       articleOpt) match {
        case (Some(Success(resource)), Some(article)) =>
          val taxonomyResource = article.flatMap(a =>
            taxononyApiClient.updateResource(resource.copy(contentUri = Some(s"urn:article:${a.articleId}"))))
          taxonomyResource
            .map(r => s"/subjects${r.path}")
            .orElse(article.map(a => s"/article/${a.articleId}"))
        case (Some(Success(_)), None) =>
          Failure(new ImportException("Failed to retrieve main node id for article"))
        case (Some(Failure(ex)), _) => Failure(ex)
        case (None, Some(article)) =>
          article.map(a => s"/article/${a.articleId}")
        case (None, None) =>
          Failure(
            new ImportException(s"Article could not be imported, and resource with node id(s) ${nodeWithTranslations
              .map(_.nid)
              .mkString(",")} does not exist in taxonomy"))
      }
    }

    private[service] def oldToNewLicenseKey(license: String): Option[LicenseDefinition] = {
      val licenses = Map(
        "by" -> "CC-BY-4.0",
        "by-sa" -> "CC-BY-SA-4.0",
        "by-nc" -> "CC-BY-NC-4.0",
        "by-nd" -> "CC-BY-ND-4.0",
        "by-nc-sa" -> "CC-BY-NC-SA-4.0",
        "by-nc-nd" -> "CC-BY-NC-ND-4.0",
        "by-3.0" -> "CC-BY-4.0",
        "by-sa-3.0" -> "CC-BY-SA-4.0",
        "by-nc-3.0" -> "CC-BY-NC-4.0",
        "by-nd-3.0" -> "CC-BY-ND-4.0",
        "by-nc-sa-3.0" -> "CC-BY-NC-SA-4.0",
        "by-nc-nd-3.0" -> "CC-BY-NC-ND-4.0",
        "copyrighted" -> "COPYRIGHTED",
        "cc0" -> "CC0-1.0",
        "pd" -> "PD",
        "nolaw" -> "CC0-1.0",
        "noc" -> "PD"
      )
      val newLicense = getLicense(licenses.getOrElse(license, license))
      if (newLicense.isEmpty) {
        throw new ImportException(s"License $license is not supported.")
      }
      newLicense
    }

    private def asLearningStepType(stepType: String): StepType.Value = {
      stepType match {
        case "1" => StepType.INTRODUCTION
        case "2" => StepType.TEXT
        case "3" => StepType.QUIZ
        case "4" => StepType.TASK
        case "5" => StepType.MULTIMEDIA
        case "6" => StepType.SUMMARY
        case "7" => StepType.TEST
        case "8" => StepType.SUMMARY
        case _   => StepType.TEXT
      }
    }

    private[service] def descriptionAsList(step: Option[Step], translations: Seq[Step]): Seq[Description] = {
      val translationDescriptions = translations
        .filter(step => step.description.isDefined && !step.description.get.isEmpty)
        .map(tr => Description(tidyUpDescription(tr.description.get), languageOrUnknown(tr.language)))
      step match {
        case Some(s) =>
          s.description.isDefined && !s.description.get.isEmpty match {
            case true =>
              Seq(Description(tidyUpDescription(s.description.get), languageOrUnknown(s.language))) ++ translationDescriptions
            case false => translationDescriptions
          }
        case None => translationDescriptions
      }
    }

    private[service] def tidyUpDescription(description: String, allowHtml: Boolean = true): String = {
      Option(description) match {
        case None => ""
        case Some(desc) =>
          HtmlCleaner.cleanHtml(desc.replaceAll("\\s", " "), allowHtml)
      }
    }

    private[service] def embedUrlsAsList(step: Step, translations: Seq[Step]): Seq[EmbedUrl] = {
      (Seq(step) ++ translations).collect {
        case Step(_, _, _, _, _, _, Some(embedUrl), _, _, language) =>
          EmbedUrl(embedUrl, languageOrUnknown(language), EmbedType.OEmbed)
      }
    }

    /**
      * Gets and merges tags for for nodeIds specified languages.
      *
      * @param nodeIds nodeIds to get tags for
      * @param languages languages to get tags for
      * @return Fetched tags
      */
    private def getTags(nodeIds: Seq[Long], languages: Seq[String]): Seq[LearningPathTags] = {
      nodeIds
        .flatMap(keywordsService.forNodeId)
        .groupBy(_.language)
        .map { case (lang, t) => LearningPathTags(t.flatMap(_.tags).distinct, lang) }
        .filter(t => languages.contains(t.language)) // Only include relevant tags in relevant languages
        .toSeq
    }

    private[service] def asLearningPath(mainImport: MainPackageImport,
                                        imageUrl: Option[ImageMetaInformation],
                                        clientId: String): LearningPath = {
      val mainPackage = mainImport.mainPackage
      val duration = Some((mainImport.mainPackage.durationHours * 60) + mainPackage.durationMinutes)
        .map(Math.max(1, _))
      val lastUpdated = mainPackage.lastUpdated

      val descriptions = Seq(
        Description(tidyUpDescription(mainImport.mainPackage.description, allowHtml = false),
                    languageOrUnknown(mainImport.mainPackage.language))) ++
        mainImport.translations.map(tr =>
          Description(tidyUpDescription(tr.description, allowHtml = false), languageOrUnknown(tr.language)))

      val titles = Seq(Title(mainImport.mainPackage.title, languageOrUnknown(mainImport.mainPackage.language))) ++
        mainImport.translations.map(tr => Title(tr.title, languageOrUnknown(tr.language)))

      val tags = getTags(mainImport.mainPackage.nid +: mainImport.translations.map(_.nid),
                         Language.findSupportedLanguages(titles, descriptions))

      val learningSteps = mainImport.mainPackage.steps.map(step =>
        asLearningStep(step, mainImport.translations.flatMap(_.steps).filter(_.pos == step.pos)))
      val authors = (mainPackage.authors ++ mainImport.translations.flatMap(_.authors)).distinct
        .map(a => Author(a.`type`, a.name))

      LearningPath(
        None,
        None,
        Some(s"${mainPackage.packageId}"),
        None,
        titles,
        descriptions,
        imageUrl.map(_.id),
        duration,
        LearningPathStatus.PUBLISHED,
        LearningPathVerificationStatus.CREATED_BY_NDLA,
        lastUpdated,
        tags,
        clientId,
        Copyright(mainPackage.license, authors),
        learningSteps
      )
    }

    private def asLearningPath(existing: LearningPath, toUpdate: LearningPath): LearningPath = {
      existing.copy(
        title = toUpdate.title,
        description = toUpdate.description,
        coverPhotoId = toUpdate.coverPhotoId,
        duration = toUpdate.duration,
        lastUpdated = toUpdate.lastUpdated,
        tags = toUpdate.tags,
        owner = toUpdate.owner,
        status = toUpdate.status,
        learningsteps = toUpdate.learningsteps
      )
    }

    private def asLearningStep(step: Step, translations: Seq[Step]): LearningStep = {
      val seqNo = step.pos - 1
      val stepType = asLearningStepType(s"${step.stepType}")

      val title = Seq(Title(step.title, languageOrUnknown(step.language))) ++ translations.map(translation =>
        Title(translation.title, languageOrUnknown(translation.language)))
      val descriptions = descriptionAsList(Some(step), translations)

      val embedUrls = embedUrlsAsList(step, translations)
      val showTitle = descriptions.nonEmpty

      val license = step.license.filter(_.trim.nonEmpty).flatMap(oldToNewLicenseKey).map(_.license.toString)

      LearningStep(
        None,
        None,
        Some(s"${step.pageId}"),
        None,
        seqNo,
        title,
        descriptions,
        embedUrls,
        stepType,
        license,
        showTitle
      )
    }

    private def asLearningStep(existing: LearningStep, toUpdate: LearningStep): LearningStep = {
      existing.copy(
        seqNo = toUpdate.seqNo,
        title = toUpdate.title,
        description = toUpdate.description,
        embedUrl = toUpdate.embedUrl,
        `type` = toUpdate.`type`,
        license = toUpdate.license
      )
    }

  }
}
