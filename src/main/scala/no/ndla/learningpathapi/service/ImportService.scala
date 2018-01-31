/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import com.netaporter.uri.dsl._
import no.ndla.learningpathapi.integration.{KeywordsServiceComponent, _}
import no.ndla.learningpathapi.model.api.{ImportReport, ImportStatus, LearningPathSummaryV2}
import no.ndla.learningpathapi.model.domain.Language.languageOrUnknown
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexServiceComponent
import no.ndla.mapping.License._

import scala.util.{Failure, Success, Try}


trait ImportService {
  this: LearningPathRepositoryComponent
    with KeywordsServiceComponent
    with ImageApiClientComponent
    with MigrationApiClient
    with SearchIndexServiceComponent
    with ConverterServiceComponent
    with ArticleImportClient
    with TaxonomyApiClient =>
  val importService: ImportService

  class ImportService {
    private val NdlaDomains = Seq("ndla.no", "red.ndla.no")

    def doImport(nodeId: String, clientId: String): Try[ImportReport] = {
      val learningpathSummary = for {
        metaData <- migrationApiClient.getLearningPath(nodeId)
        converted <- upload(nodeId, metaData, clientId)
        _ <- searchIndexService.indexDocument(converted)
        summary <- converterService.asApiLearningpathSummaryV2(converted)
      } yield summary

      generateImportReport(nodeId, learningpathSummary)
    }

    private def generateImportReport(nodeId: String, learningPathSummaryV2: Try[LearningPathSummaryV2]): Try[ImportReport] = {
      learningPathSummaryV2 match {
        case Success(l) => Success(ImportReport(nodeId, ImportStatus.OK, Seq.empty, Some(l.id)))
        case Failure(ex) => Failure(ex)
      }
    }

    private[service] def upload(nodeId: String, mainImport: MainPackageImport, clientId: String): Try[LearningPath] = {
      val embedUrls = (mainImport.translations :+ mainImport.mainPackage)
        .flatMap(_.steps).flatMap(_.embedUrl)
      val (articleImports, failedImports) = importArticles(embedUrls).partition {
        case (_, importStatus) => importStatus.isSuccess
      }

      if (failedImports.nonEmpty) {
        val messages = failedImports.map(a => a._2.failed.get.getMessage)
        Failure(ImportReport(nodeId, ImportStatus.ERROR, messages, None))
      } else {
        val embedUrlMap = articleImports.map { case (oldUrl, taxonomyResource) => oldUrl -> taxonomyResource.get.path }.toMap

        val coverPhoto = mainImport.mainPackage.imageNid.flatMap(importCoverPhoto)
        val learningPathWithOldEmbedUrls = asLearningPath(mainImport, coverPhoto, clientId)

        val steps = learningPathWithOldEmbedUrls.learningsteps.map {
          case l: LearningStep if l.embedUrl.nonEmpty =>
            val embedUrls = l.embedUrl.map(em => em.copy(url = embedUrlMap.getOrElse(em.url, em.url))) // Fall back on old ndla url if article could not be imported or has no entry in taxonomy
            l.copy(embedUrl = embedUrls)
          case l => l
        }
        val learningPathWithNewEmbedUrls = learningPathWithOldEmbedUrls.copy(learningsteps = steps)

        val persisted = learningPathRepository.withExternalId(learningPathWithNewEmbedUrls.externalId) match {
          case None => learningPathRepository.insert(learningPathWithNewEmbedUrls)
          case Some(existingLearningPath) =>
            learningPathRepository.update(asLearningPath(existingLearningPath, learningPathWithNewEmbedUrls))
            learningPathWithNewEmbedUrls.learningsteps.foreach(learningStep => {
              learningPathRepository.learningStepWithExternalIdAndForLearningPath(learningStep.externalId, existingLearningPath.id) match {
                case None => learningPathRepository.insertLearningStep(learningStep.copy(learningPathId = existingLearningPath.id))
                case Some(existingLearningStep) => learningPathRepository.updateLearningStep(asLearningStep(existingLearningStep, learningStep))
              }
            })
            existingLearningPath
        }
        Success(persisted)
      }
    }

    private def importCoverPhoto(imageNid: Int): Option[ImageMetaInformation] = {
      imageApiClient.imageMetaWithExternalId(imageNid.toString)
        .orElse(imageApiClient.importImage(imageNid.toString))
    }

    private def importArticles(embedUrls: Seq[String]): Seq[(String, Try[TaxonomyResource])] = {
      def getNodeIdFromUrl = (url: String) => url.path.split("/").lastOption
      val urlToMainNidMap = embedUrls.map(url => {
        if (NdlaDomains.contains(url.host.getOrElse(""))) {
          url -> getNodeIdFromUrl(url).map(migrationApiClient.getAllNodeIds)
        } else {
          url -> None
        }
      }).filter(_._2.isDefined).toMap

      val ass = urlToMainNidMap.values
        .map(_.getOrElse(Set.empty))
        .map(nids => nids -> importAndUpdateTaxonomy(nids)).toMap

      embedUrls.map(b => {
        val nodeIdsForArticle = urlToMainNidMap.get(b).flatten.getOrElse(Set.empty)
        val taxonomyResourceForArticle = ass(nodeIdsForArticle)
        b -> taxonomyResourceForArticle
      })
    }

    private def importAndUpdateTaxonomy(nodeWithTranslations: Set[ArticleMigrationContent]): Try[TaxonomyResource] = {
      val mainNodeIdOpt = nodeWithTranslations.find(_.isMainNode)

      (nodeWithTranslations.map(c => taxononyApiClient.getResource(c.nid)).find(_.isSuccess), mainNodeIdOpt) match {
        case (Some(Success(resource)), Some(mainNodeId)) =>
          for {
            a <- articleImportClient.importArticle(mainNodeId.nid)
            taxonomyResource <- taxononyApiClient.updateResource(resource.copy(contentUri=Some(s"urn:article:${a.articleId}")))
          } yield taxonomyResource
        case (Some(Success(_)), None) => Failure(new ImportException("Failed to retrieve main node id for article"))
        case (Some(Failure(ex)), _) => Failure(ex)
        case (None, _) => Failure(new ImportException(s"Resource with node id(s) ${nodeWithTranslations.map(_.nid).mkString(",")} does not exist in taxonomy"))
      }
    }

    private[service] def oldToNewLicenseKey(license: String): String = {
      val licenses = Map("nolaw" -> "cc0", "noc" -> "pd")
      val newLicense = licenses.getOrElse(license, license)
      if (getLicense(newLicense).isEmpty) {
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
        case _ => StepType.TEXT
      }
    }

    private[service] def descriptionAsList(step: Option[Step], translations: Seq[Step]): Seq[Description] = {
      val translationDescriptions = translations.filter(step => step.description.isDefined && !step.description.get.isEmpty).map(tr => Description(tidyUpDescription(tr.description.get), languageOrUnknown(tr.language)))
      step match {
        case Some(s) =>
          s.description.isDefined && !s.description.get.isEmpty match {
            case true => Seq(Description(tidyUpDescription(s.description.get), languageOrUnknown(s.language))) ++ translationDescriptions
            case false => translationDescriptions
          }
        case None => translationDescriptions
      }
    }

    private[service] def tidyUpDescription(description: String, allowHtml: Boolean = true): String = {
      Option(description) match {
        case None => ""
        case Some(desc) => HtmlCleaner.cleanHtml(desc.replaceAll("\\s", " "), allowHtml)
      }
    }

    private[service] def embedUrlsAsList(step: Step, translations: Seq[Step]): Seq[EmbedUrl] = {
      (Seq(step) ++ translations).collect {
        case Step(_, _, _, _, _, _, Some(embedUrl), _, _, language) if NdlaDomains.contains(embedUrl.host.getOrElse("")) =>
          EmbedUrl(embedUrl, languageOrUnknown(language), EmbedType.OEmbed)
      }
    }

    private[service] def asLearningPath(mainImport: MainPackageImport, imageUrl: Option[ImageMetaInformation], clientId: String): LearningPath = {
      val mainPackage = mainImport.mainPackage
      val duration = Some((mainImport.mainPackage.durationHours * 60) + mainPackage.durationMinutes)
      val lastUpdated = mainPackage.lastUpdated


      val descriptions = Seq(Description(tidyUpDescription(mainImport.mainPackage.description, allowHtml = false), languageOrUnknown(mainImport.mainPackage.language))) ++
        mainImport.translations.map(tr => Description(tidyUpDescription(tr.description, allowHtml = false), languageOrUnknown(tr.language)))

      val titles = Seq(Title(mainImport.mainPackage.title, languageOrUnknown(mainImport.mainPackage.language))) ++
        mainImport.translations.map(tr => Title(tr.title, languageOrUnknown(tr.language)))

      val tags = keywordsService.forNodeId(mainImport.mainPackage.nid) ++
        mainImport.translations.flatMap(tr => keywordsService.forNodeId(tr.nid))

      val learningSteps = mainImport.mainPackage.steps.map(step => asLearningStep(step, mainImport.translations.flatMap(_.steps).filter(_.pos == step.pos)))

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
        Copyright("by-sa", Seq()), // TODO: Verify with NDLA what to use as default license on imported learningpaths.
        learningSteps)
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
        status = toUpdate.status
      )
    }

    private def asLearningStep(step: Step, translations: Seq[Step]): LearningStep = {
      val seqNo = step.pos - 1
      val stepType = asLearningStepType(s"${step.stepType}")

      val title = Seq(Title(step.title, step.language)) ++ translations.map(translation => Title(translation.title, languageOrUnknown(translation.language)))
      val descriptions = descriptionAsList(Some(step), translations)

      val embedUrls = embedUrlsAsList(step, translations)
      val showTitle = descriptions.nonEmpty

      LearningStep(None, None, Some(s"${step.pageId}"), None, seqNo, title, descriptions, embedUrls, stepType, step.license.map(oldToNewLicenseKey), showTitle)
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
