/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.{KeywordsServiceComponent, _}
import no.ndla.learningpathapi.model.api.{ImportReport, LearningPathSummary}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexServiceComponent

import scala.util.{Failure, Success, Try}


trait ImportServiceComponent {
  this: LearningPathRepositoryComponent with KeywordsServiceComponent with ImageApiClientComponent with MigrationApiClient with SearchIndexServiceComponent with ConverterServiceComponent =>
  val importService: ImportService

  val ChristerTest = "K2pcS0O04OiICIyGcQPZL9g0"
  val ChristerStaging = "K2pcS0O04OiICIyGcQPZL9g0"
  val ChristerProd = "K2pcS0O04OiICIyGcQPZL9g0"

  class ImportService {

    def importAll: Try[Seq[ImportReport]] = {
      migrationApiClient.getAllLearningPathIds match {
        case Failure(f) => Failure(f)
        case Success(liste) => Success(liste
            .map(id => (id, doImport(id)))
            .map(tuple => ImportReport(tuple._1, getStatus(tuple._2))))
      }
    }

    def getStatus(learningPathSummaryTry: Try[LearningPathSummary]): String = {
      learningPathSummaryTry match {
        case Success(learningPathSummary) => "OK"
        case Failure(f) => f.getMessage
      }
    }

    def doImport(nodeId: String): Try[LearningPathSummary] = {
      for {
        metaData <- migrationApiClient.getLearningPath(nodeId)
        converted <- Try(upload(metaData))
        indexed <- searchIndexService.indexDocument(converted)
      } yield converterService.asApiLearningpathSummary(converted)
    }

    def upload(mainImport: MainPackageImport): LearningPath = {
      val coverPhoto = mainImport.mainPackage.imageNid.flatMap(img => {
        imageApiClient.imageMetaWithExternalId(img.toString) match {
          case Some(image) => Some(image)
          case None => imageApiClient.importImage(img.toString)
        }
      })

      val descriptions = Seq(Description(tidyUpDescription(mainImport.mainPackage.description, allowHtml = false), Some(mainImport.mainPackage.language))) ++
        mainImport.translations.map(tr => Description(tidyUpDescription(tr.description, allowHtml = false), Some(tr.language)))

      val titles = Seq(Title(mainImport.mainPackage.title, Some(mainImport.mainPackage.language))) ++
        mainImport.translations.map(tr => Title(tr.title, Some(tr.language)))

      val tags = keywordsService.forNodeId(mainImport.mainPackage.nid) ++
        mainImport.translations.flatMap(tr => keywordsService.forNodeId(tr.nid))

      val steps = mainImport.mainPackage.steps.map(step => asLearningStep(step, mainImport.translations.flatMap(_.steps).filter(_.pos == step.pos)))

      val learningPath = asLearningPath(mainImport.mainPackage, titles, descriptions, tags, steps, coverPhoto)

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

      val title = Seq(Title(step.title, Some(step.language))) ++ translations.map(translation => Title(translation.title, Some(translation.language)))
      val descriptions = descriptionAsList(Some(step), translations)

      val embedUrls = embedUrlsAsList(step, translations)
      val showTitle = descriptions.nonEmpty

      LearningStep(None, None, Some(s"${step.pageId}"), None, seqNo, title, descriptions, embedUrls, stepType, step.license, showTitle)
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
      val translationDescriptions = translations.filter(step => step.description.isDefined && !step.description.get.isEmpty).map(tr => Description(tidyUpDescription(tr.description.get), Some(tr.language)))
      step match {
        case Some(s) => {
          s.description.isDefined && !s.description.get.isEmpty match {
            case true => Seq(Description(tidyUpDescription(s.description.get), Some(s.language))) ++ translationDescriptions
            case false => translationDescriptions
          }
        }
        case None => translationDescriptions
      }
    }

    def embedUrlsAsList(step: Step, translations: Seq[Step]): Seq[EmbedUrl] = {
      val translationUrls = translations.filter(step => step.embedUrlToNdlaNo.isDefined).map(url => EmbedUrl(url.embedUrlToNdlaNo.get, Some(url.language), EmbedType.OEmbed))
      step.embedUrlToNdlaNo match {
        case None => translationUrls
        case Some(url) => {
          Seq(EmbedUrl(url, Some(step.language), EmbedType.OEmbed)) ++ translationUrls
        }
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

    def asLearningPath(pakke: Package, titles: Seq[Title], descriptions: Seq[Description], tags: Seq[LearningPathTags], learningSteps: Seq[LearningStep], imageUrl: Option[ImageMetaInformation]) = {
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
        getOwnerForEnvironment(LearningpathApiProperties.Environment),
        Copyright("by-sa", Seq()), // TODO: Verify with NDLA what to use as default license on imported learningpaths.
        learningSteps)
    }

    def getOwnerForEnvironment(environment: String): String = {
      environment match {
        case "prod" => ChristerProd
        case "staging" => ChristerStaging
        case _ => ChristerTest
      }
    }
  }
}
