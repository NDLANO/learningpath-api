package no.ndla.learningpathapi.batch.service

import no.ndla.learningpathapi.batch._
import no.ndla.learningpathapi.batch.integration.{CMDataComponent, KeywordsServiceComponent, PackageDataComponent}
import no.ndla.learningpathapi.integration.{ImageApiClientComponent, ImageMetaInformation}
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent


trait ImportServiceComponent {
  this: CMDataComponent with PackageDataComponent with LearningPathRepositoryComponent with KeywordsServiceComponent with ImageApiClientComponent =>
  val importService: ImportService

  val ChristerTest = "410714a7-c09d-4e9a-9595-e7f13e19c463"
  val ChristerStaging = "2dd7a2cd-b71b-4ea1-b9a3-efa4c49613ab"
  val ChristerProd = "unknown"

  class ImportService {

    def doImport(environment: String) = {
      val nodes: List[Node] = cmData.allLearningPaths()
      val nodesToImport = nodes.filterNot(_.isTranslation)
      val imagesToImport = nodesToImport.flatMap(_.imageNid)
      val imageExternToApiMap = imagesToImport.map(img => (img, imageApiClient.imageMetaWithExternalId(img.toString))).toMap

      logger.info("Need to import images with id: {}", imageExternToApiMap.filter(_._2.isEmpty).keys.mkString("','"))

      if(imageExternToApiMap.exists(_._2.isEmpty)){
        logger.info("All required images not found. Exiting")
        System.exit(1)
      }

      nodesToImport.foreach(node => {
        importNode(packageData.packageFor(node), getTranslations(node, nodes), node.imageNid.flatMap(id => imageExternToApiMap(id)), environment)
      })

    }

    def importNode(optPakke: Option[Package], optTranslations: List[Option[Package]], imageUrl: Option[ImageMetaInformation], environment: String) = {
      optPakke.foreach(pakke => {
        val steps = packageData.stepsForPackage(pakke)

        val descriptions = Description(tidyUpDescription(pakke.description, allowHtml = false), Some(pakke.language)) ::
          optTranslations.flatten.map(pak => Description(tidyUpDescription(pak.description, allowHtml = false), Some(pak.language)))

        val titles = Title(pakke.packageTitle, Some(pakke.language)) :: optTranslations.flatten.map(pak => Title(pak.packageTitle, Some(pak.language)))
        val tags = (keywordsService.forNodeId(pakke.nodeId) ++ optTranslations.flatten.flatMap(tra => keywordsService.forNodeId(tra.nodeId))).distinct
        val learningSteps = steps.map(step => asLearningStep(step, packageData.getTranslationSteps(optTranslations, step.pos)))

        val learningPath = asLearningPath(pakke, titles, descriptions, tags, learningSteps, imageUrl, environment)
        learningPathRepository.withExternalId(learningPath.externalId) match {
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
                    `type`= learningStep.`type`,
                    license = learningStep.license
                  ))
                }
              }
            })
          }
        }
      })
    }

    def asLearningPath(pakke: Package, titles:List[Title], descriptions:List[Description], tags: Seq[LearningPathTags], learningSteps: List[LearningStep], imageUrl: Option[ImageMetaInformation], environment: String) = {
      val duration = Some((pakke.durationHours * 60) + pakke.durationMinutes)
      val lastUpdated = pakke.lastUpdated

      val owner = environment match {
        case "test" => ChristerTest
        case "staging" => ChristerStaging
        case "prod" => ChristerProd
        case _ => ChristerTest
      }

      val imageApiPrefix = environment match {
        case "prod" => "http://api.ndla.no/images/"
        case _ => s"http://api.$environment.ndla.no/images/"
      }

      LearningPath(
        None,
        None,
        Some(s"${pakke.packageId}"),
        None,
        titles,
        descriptions,
        imageUrl.map(img => imageApiPrefix + img.id),
        duration,
        LearningPathStatus.PUBLISHED,
        LearningPathVerificationStatus.CREATED_BY_NDLA,
        lastUpdated,
        tags,
        owner,
        learningSteps)
    }

    def asLearningStep(step: Step, translations: List[Step]): LearningStep = {
      val seqNo = step.pos - 1
      val stepType = asLearningStepType(s"${step.stepType}")

      val title = Title(step.title, Some(step.language)) :: translations.map(translation => Title(translation.title, Some(translation.language)))
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

    def embedUrlsAsList(step: Step, translations: List[Step]): List[EmbedUrl] = {
      val translationUrls = translations.filter(step => step.embedUrlToNdlaNo.isDefined).map(url => EmbedUrl(url.embedUrlToNdlaNo.get, Some(url.language)))
      step.embedUrlToNdlaNo match {
        case None => translationUrls
        case Some(url) => {
          EmbedUrl(url, Some(step.language)) :: translationUrls
        }
      }
    }

    def descriptionAsList(step: Option[Step], translations: List[Step]): List[Description] = {
      val translationDescriptions = translations.filter(step => step.description.isDefined && !step.description.get.isEmpty).map(tr => Description(tidyUpDescription(tr.description.get), Some(tr.language)))
      step match {
        case Some(s) => {
          s.description.isDefined && !s.description.get.isEmpty match {
            case true => Description(tidyUpDescription(s.description.get), Some(s.language)) :: translationDescriptions
            case false => translationDescriptions
          }
        }
        case None => translationDescriptions
      }
    }

    def tidyUpDescription(description: String, allowHtml:Boolean = true): String = {
      Option(description) match {
        case None => ""
        case Some(desc) => {
          HtmlCleaner.cleanHtml(desc.replaceAll("\\s", " "), allowHtml)
        }
      }
    }

    def getTranslations(node: Node, nodes:List[Node]): List[Option[Package]] = {
      nodes.filter(candidate => candidate.tnid == node.nid && candidate.nid != node.nid).map(node => packageData.packageFor(node))
    }

    def debug(learningPath: LearningPath) = {
      println(s"${learningPath.title.head.title}")
      learningPath.learningsteps.foreach(step => {
        println(s"    (${step.seqNo}) - ${step.title.head.title} - ${step.embedUrl.headOption.getOrElse(EmbedUrl("NONE",None)).url}")
      })
    }
  }
}
