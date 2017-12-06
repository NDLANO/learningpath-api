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
import no.ndla.learningpathapi.model.api.{ImportReport, LearningPathSummaryV2}
import no.ndla.learningpathapi.model.domain.Language.languageOrUnknown
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexServiceComponent

import scala.util.{Failure, Success, Try}


trait ImportServiceComponent {
  this: LearningPathRepositoryComponent
    with KeywordsServiceComponent
    with ImageApiClientComponent
    with MigrationApiClient
    with SearchIndexServiceComponent
    with ConverterServiceComponent
    with ArticleApiClient =>
  val importService: ImportService

  class ImportService {

    def importAll(clientId: String): Try[Seq[ImportReport]] = {
      migrationApiClient.getAllLearningPathIds match {
        case Failure(f) => Failure(f)
        case Success(liste) => Success(liste
          .map(id => (id, doImport(id, clientId)))
          .map { case (nid, summary) => ImportReport(nid, getStatus(summary)) })
      }
    }

    def getStatus(learningPathSummaryTry: Try[LearningPathSummaryV2]): String = {
      learningPathSummaryTry match {
        case Success(_) => "OK"
        case Failure(f) => f.getMessage
      }
    }

    def doImport(nodeId: String, clientId: String): Try[LearningPathSummaryV2] = {
      for {
        metaData <- migrationApiClient.getLearningPath(nodeId)
        converted <- Try(upload(metaData, clientId))
        indexed <- searchIndexService.indexDocument(converted)
        summary <- converterService.asApiLearningpathSummaryV2(converted)
      } yield summary
    }

    def upload(mainImport: MainPackageImport, clientId: String): LearningPath = {
      val coverPhoto = mainImport.mainPackage.imageNid.flatMap(img => {
        imageApiClient.imageMetaWithExternalId(img.toString) match {
          case Some(image) => Some(image)
          case None => imageApiClient.importImage(img.toString)
        }
      })

      val descriptions = Seq(Description(tidyUpDescription(mainImport.mainPackage.description, allowHtml = false), languageOrUnknown(mainImport.mainPackage.language))) ++
        mainImport.translations.map(tr => Description(tidyUpDescription(tr.description, allowHtml = false), languageOrUnknown(tr.language)))

      val titles = Seq(Title(mainImport.mainPackage.title, languageOrUnknown(mainImport.mainPackage.language))) ++
        mainImport.translations.map(tr => Title(tr.title, languageOrUnknown(tr.language)))

      val tags = keywordsService.forNodeId(mainImport.mainPackage.nid) ++
        mainImport.translations.flatMap(tr => keywordsService.forNodeId(tr.nid))

      val steps = mainImport.mainPackage.steps.map(step => asLearningStep(step, mainImport.translations.flatMap(_.steps).filter(_.pos == step.pos)))

      val learningPath = asLearningPath(mainImport.mainPackage, titles, descriptions, tags, steps, coverPhoto, clientId)

      val persisted = learningPathRepository.withExternalId(learningPath.externalId) match {
        case None => {
          learningPathRepository.insert(learningPath)
        }
        case Some(existingLearningPath) => {
          learningPathRepository.update(existingLearningPath.copy(
            title = learningPath.title,
            description = learningPath.description,
            coverPhotoId = learningPath.coverPhotoId,
            duration = learningPath.duration,
            lastUpdated = learningPath.lastUpdated,
            tags = learningPath.tags,
            owner = learningPath.owner,
            status = learningPath.status
          ))

          learningPath.learningsteps.foreach(learningStep => {
            learningPathRepository.learningStepWithExternalIdAndForLearningPath(learningStep.externalId, existingLearningPath.id) match {
              case None => {
                learningPathRepository.insertLearningStep(learningStep.copy(learningPathId = existingLearningPath.id))
              }
              case Some(existingLearningStep) => {
                learningPathRepository.updateLearningStep(existingLearningStep.copy(
                  seqNo = learningStep.seqNo,
                  title = learningStep.title,
                  description = learningStep.description,
                  embedUrl = learningStep.embedUrl,
                  `type` = learningStep.`type`,
                  license = learningStep.license
                ))
              }
            }
          })
          existingLearningPath
        }
      }
      persisted
    }

    def asLearningStep(step: Step, translations: Seq[Step]): LearningStep = {
      val seqNo = step.pos - 1
      val stepType = asLearningStepType(s"${step.stepType}")

      val title = Seq(Title(step.title, step.language)) ++ translations.map(translation => Title(translation.title, languageOrUnknown(translation.language)))
      val descriptions = descriptionAsList(Some(step), translations)

      val embedUrls = embedUrlsAsList(step, translations)
      val showTitle = descriptions.nonEmpty

      importArticlesUsedInLearningStep(embedUrls)

      LearningStep(None, None, Some(s"${step.pageId}"), None, seqNo, title, descriptions, embedUrls, stepType, step.license, showTitle)
    }

    def importArticlesUsedInLearningStep(embedUrls: Seq[EmbedUrl]): Unit = {
      val ndlaDomains = Seq("ndla.no", "red.ndla.no")
      embedUrls.collect {
        case (embedUrl) if ndlaDomains.contains(embedUrl.url.host.getOrElse("")) =>
          val nodeId = embedUrl.url.path.split("/").lastOption
          nodeId.foreach(articleApiClient.importArticle)
        case _ =>
      }
    }

    def asLearningStepType(stepType: String): StepType.Value = {
      stepType match {
        case "1" => StepType.INTRODUCTION
        case "2" => StepType.TEXT
        case "3" => StepType.QUIZ
        case "4" => StepType.TASK
        case "5" => StepType.MULTIMEDIA
        case "6" => StepType.SUMMARY
        case "7" => StepType.TEST
        case "8" => StepType.SUMMARY
        case default => StepType.TEXT
      }
    }


    def descriptionAsList(step: Option[Step], translations: Seq[Step]): Seq[Description] = {
      val translationDescriptions = translations.filter(step => step.description.isDefined && !step.description.get.isEmpty).map(tr => Description(tidyUpDescription(tr.description.get), languageOrUnknown(tr.language)))
      step match {
        case Some(s) => {
          s.description.isDefined && !s.description.get.isEmpty match {
            case true => Seq(Description(tidyUpDescription(s.description.get), languageOrUnknown(s.language))) ++ translationDescriptions
            case false => translationDescriptions
          }
        }
        case None => translationDescriptions
      }
    }

    def tidyUpDescription(description: String, allowHtml: Boolean = true): String = {
      Option(description) match {
        case None => ""
        case Some(desc) => {
          HtmlCleaner.cleanHtml(desc.replaceAll("\\s", " "), allowHtml)
        }
      }
    }

    def embedUrlsAsList(step: Step, translations: Seq[Step]): Seq[EmbedUrl] = {
      val translationUrls = translations.filter(step => step.embedUrlToNdlaNo.isDefined).map(url => EmbedUrl(url.embedUrlToNdlaNo.get, languageOrUnknown(url.language), EmbedType.OEmbed))
      step.embedUrlToNdlaNo match {
        case None => translationUrls
        case Some(url) => {
          Seq(EmbedUrl(url, languageOrUnknown(step.language), EmbedType.OEmbed)) ++ translationUrls
        }
      }
    }

    def asLearningPath(pakke: Package, titles: Seq[Title], descriptions: Seq[Description], tags: Seq[LearningPathTags], learningSteps: Seq[LearningStep], imageUrl: Option[ImageMetaInformation], clientId: String) = {
      val duration = Some((pakke.durationHours * 60) + pakke.durationMinutes)
      val lastUpdated = pakke.lastUpdated

      LearningPath(
        None,
        None,
        Some(s"${pakke.packageId}"),
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

  }

}
