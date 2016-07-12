package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.{KeywordsServiceComponent, _}
import no.ndla.learningpathapi.model.api.LearningPathSummary
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexServiceComponent

import scala.util.{Failure, Success, Try}


trait ImportServiceComponent {
  this: LearningPathRepositoryComponent with KeywordsServiceComponent with ImageApiClientComponent with MigrationApiClient with SearchIndexServiceComponent with ConverterServiceComponent =>
  val importService: ImportService

  val ChristerTest = "410714a7-c09d-4e9a-9595-e7f13e19c463"
  val ChristerStaging = "2dd7a2cd-b71b-4ea1-b9a3-efa4c49613ab"
  val ChristerProd = "unknown"

  class ImportService {

    def doImport(nodeId: String): Try[LearningPathSummary] = {
      for {
        metaData <- migrationApiClient.getLearningPath(nodeId)
        converted <- Try(upload(metaData))
        indexed <- indexLearningPath(converted)
      } yield converterService.asApiLearningpathSummary(converted)
    }

    def indexLearningPath(learningPath: LearningPath): Try[Unit] = {
      Try(searchIndexService.indexLearningPath(learningPath)) match {
        case Success(_) => Success()
        case Failure(f) => {
          logger.warn(s"Could not add learningpath with id ${learningPath.id} to search index. Try recreating the index. The error was ${f.getMessage}")
          Success()
        }
      }
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
            coverPhotoMetaUrl = learningPath.coverPhotoMetaUrl,
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

      LearningStep(None, None, Some(s"${step.pageId}"), None, seqNo, title, descriptions, embedUrls, stepType, None, showTitle)
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
      val translationUrls = translations.filter(step => step.embedUrlToNdlaNo.isDefined).map(url => EmbedUrl(url.embedUrlToNdlaNo.get, Some(url.language)))
      step.embedUrlToNdlaNo match {
        case None => translationUrls
        case Some(url) => {
          Seq(EmbedUrl(url, Some(step.language))) ++ translationUrls
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
        imageUrl.map(_.metaUrl),
        duration,
        LearningPathStatus.PUBLISHED,
        LearningPathVerificationStatus.CREATED_BY_NDLA,
        lastUpdated,
        tags,
        getOwnerForEnvironment(LearningpathApiProperties.Domain),
        learningSteps)
    }

    def getOwnerForEnvironment(environment: String): String = {
      environment match {
        case s if s.contains("api.ndla.no") => ChristerProd
        case s if s.contains("api.staging.ndla.no") => ChristerStaging
        case _ => ChristerTest
      }
    }

    def debug(learningPath: LearningPath) = {
      learningPath.title.foreach(title => logger.info(s"TITLE -- ${title.title} - ${title.language}"))
      learningPath.learningsteps.foreach(step => {
        logger.info(s"    (${step.seqNo}) - ${step.title.head.title} - ${step.embedUrl.headOption.getOrElse(EmbedUrl("NONE", None)).url}")
        step.title.tail.foreach(title => logger.info(s"       ${title.title} -- ${title.language}"))
      })
    }
  }

}
