package no.ndla.learningpathapi.batch.service

import no.ndla.learningpathapi.batch._
import no.ndla.learningpathapi.batch.integration.{CMDataComponent, KeywordsServiceComponent, PackageDataComponent}
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent


trait ImportServiceComponent {
  this: CMDataComponent with PackageDataComponent with LearningPathRepositoryComponent with KeywordsServiceComponent =>
  val importService: ImportService

  class ImportService {
    val placeholderShortText = "Kort beskrivelse på ikke mer enn 150 tegn. Sier noe fornuftig om læringsstien. Denne beskrivelsen her er på nøyaktig hundrede og femti tegn........nå."

    def doImport() = {
      val nodes: List[Node] = cmData.allLearningPaths()
      nodes.filterNot(_.isTranslation).foreach(node => {
        importNode(packageData.packageFor(node), getTranslations(node, nodes), cmData.imagePathForNid(node.imageNid))
      })
    }

    def importNode(optPakke: Option[Package], optTranslations: List[Option[Package]], imageUrl: Option[String]) = {
      optPakke.foreach(pakke => {
        val steps = packageData.stepsForPackage(pakke)

        val descriptions = List(Description(placeholderShortText, Some("nb")))
        val titles = Title(pakke.packageTitle, Some(pakke.language)) :: optTranslations.flatten.map(pak => Title(pak.packageTitle, Some(pak.language)))
        val tags = (keywordsService.forNodeId(pakke.nodeId) ::: optTranslations.flatten.flatMap(tra => keywordsService.forNodeId(tra.nodeId))).distinct
        val learningSteps = steps.map(step => asLearningStep(step, packageData.getTranslationSteps(optTranslations, step.pos)))

        val learningPath = asLearningPath(pakke, titles, descriptions, tags, learningSteps, imageUrl)
        learningPathRepository.withExternalId(learningPath.externalId) match {
          case None => {
            learningPathRepository.insert(learningPath)
          }
          case Some(existingLearningPath) => {
            learningPathRepository.update(existingLearningPath.copy(
              title = learningPath.title,
              description = learningPath.description,
              coverPhotoUrl = learningPath.coverPhotoUrl,
              duration = learningPath.duration,
              lastUpdated = learningPath.lastUpdated,
              tags = learningPath.tags,
              owner = learningPath.owner
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

    def asLearningPath(pakke: Package, titles:List[Title], descriptions:List[Description], tags: List[LearningPathTag], learningSteps: List[LearningStep], imageUrl: Option[String]) = {
      val duration = Some((pakke.durationHours * 60) + pakke.durationMinutes)
      val lastUpdated = pakke.lastUpdated

      val owner = pakke.nodeId match {
        case 149862 => "6e74cde7-1e83-49c8-8dcd-9bbef458f477" //Christer
        case 156729 => "d6b2bbd0-2dd4-485a-9d9a-af2e7c9d57ad" //RST
        case 156987 => "ddd2ff24-616a-484d-8172-55ddba52cd7a" //KW
        case 149007 => "a62debc3-74a7-43f3-88c9-d35837a41698" //Remi
        case 143822 => "e646b7f6-60ce-4364-9e77-2a88754b95db" // KES
        case default => "6e74cde7-1e83-49c8-8dcd-9bbef458f477" //Christer
      }

      val publishStatus = pakke.nodeId match {
        case 149862 => LearningPathStatus.PRIVATE
        case 156729 => LearningPathStatus.PRIVATE
        case 156987 => LearningPathStatus.PRIVATE
        case 149007 => LearningPathStatus.PRIVATE
        case 143822 => LearningPathStatus.PRIVATE
        case default => LearningPathStatus.PUBLISHED
      }

      LearningPath(
        None,
        Some(s"${pakke.packageId}"),
        titles,
        descriptions,
        imageUrl,
        duration,
        publishStatus,
        LearningPathVerificationStatus.CREATED_BY_NDLA,
        lastUpdated,
        tags,
        owner,
        learningSteps)
    }

    def asLearningStep(step: Step, translations: List[Step]): LearningStep = {
      val seqNo = step.pos
      val stepType = s"${step.stepType}"

      val title = Title(step.title, Some(step.language)) :: translations.map(translation => Title(translation.title, Some(translation.language)))
      val descriptions = descriptionAsList(Some(step), translations)

      val embedUrls = embedUrlsAsList(step, translations)

      LearningStep(None, Some(s"${step.pageId}"), None, seqNo, title, descriptions, embedUrls, asLearningStepType(stepType), None)
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

    def tidyUpDescription(description: String): String = {
      Option(description) match {
        case None => ""
        case Some(desc) => {
          HtmlCleaner.cleanHtml(desc.replaceAll("(\\r|\\n|\\t)", ""))
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
