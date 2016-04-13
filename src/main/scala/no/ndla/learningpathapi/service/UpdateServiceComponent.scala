package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.LearningPathVerificationStatus
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexServiceComponent


trait UpdateServiceComponent {
  this: LearningPathRepositoryComponent with ConverterServiceComponent with SearchIndexServiceComponent =>
  val updateService: UpdateService

  class UpdateService {
    def addLearningPath(newLearningPath: NewLearningPath, owner: String): LearningPath = {
      val learningPath = model.LearningPath(None,None,
        newLearningPath.title.map(converterService.asTitle),
        newLearningPath.description.map(converterService.asDescription),
        newLearningPath.coverPhotoUrl,
        newLearningPath.duration, model.LearningPathStatus.PRIVATE,
        LearningPathVerificationStatus.EXTERNAL,
        new Date(), newLearningPath.tags.map(converterService.asLearningPathTag), owner, List())

      converterService.asApiLearningpath(learningPathRepository.insert(learningPath))
    }

    def updateLearningPath(id: Long, newLearningPath: NewLearningPath, owner: String): Option[LearningPath] = {
      withIdAndAccessGranted(id, owner) match {
        case None => None
        case Some(existing) => {
          val toUpdate = existing.copy(
            title = newLearningPath.title.map(converterService.asTitle),
            description = newLearningPath.description.map(converterService.asDescription),
            coverPhotoUrl = newLearningPath.coverPhotoUrl,
            duration = newLearningPath.duration,
            tags = newLearningPath.tags.map(converterService.asLearningPathTag),
            lastUpdated = new Date())

          val updatedLearningPath = learningPathRepository.update(toUpdate)
          if (updatedLearningPath.isPublished) {
            searchIndexService.indexLearningPath(updatedLearningPath)
          }

          Some(converterService.asApiLearningpath(updatedLearningPath))
        }
      }
    }

    def updateLearningPathStatus(learningPathId: Long, status: LearningPathStatus, owner: String): Option[LearningPath] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => None
        case Some(existing) => {
          val newStatus = model.LearningPathStatus.valueOfOrDefault(status.status)
          if(newStatus == model.LearningPathStatus.PUBLISHED){
            existing.validateForPublishing()
          }

          val updatedLearningPath = learningPathRepository.update(
            existing.copy(
              status = newStatus,
              lastUpdated = new Date()))

          updatedLearningPath.isPublished match {
            case true => searchIndexService.indexLearningPath(updatedLearningPath)
            case false => searchIndexService.deleteLearningPath(updatedLearningPath)
          }


          Some(converterService.asApiLearningpath(updatedLearningPath))
        }
      }
    }

    def deleteLearningPath(learningPathId: Long, owner: String): Boolean = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => false
        case Some(existing) => {
          learningPathRepository.delete(learningPathId)
          searchIndexService.deleteLearningPath(existing)
          true
        }
      }
    }

    def addLearningStep(learningPathId: Long, newLearningStep: NewLearningStep, owner: String): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => None
        case Some(learningPath) => {
          val newSeqNo = learningPath.learningsteps.isEmpty match {
            case true => 1
            case false => learningPath.learningsteps.map(_.seqNo).max + 1
          }

          val newStep = model.LearningStep(
            None,
            None,
            learningPath.id,
            newSeqNo,
            newLearningStep.title.map(converterService.asTitle),
            newLearningStep.description.map(converterService.asDescription),
            newLearningStep.embedUrl.map(converterService.asEmbedUrl),
            model.StepType.valueOfOrDefault(newLearningStep.`type`),
            newLearningStep.license)

          val insertedStep = learningPathRepository.insertLearningStep(newStep)
          val updatedPath = learningPathRepository.update(learningPath.copy(
            learningsteps = learningPath.learningsteps :+ insertedStep,
            lastUpdated = new Date()))

          if (updatedPath.isPublished) {
            searchIndexService.indexLearningPath(updatedPath)
          }

          Some(converterService.asApiLearningStep(insertedStep, updatedPath))
        }
      }
    }

    def updateLearningStep(learningPathId: Long, learningStepId: Long, newLearningStep: NewLearningStep, owner: String): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => None
        case Some(learningPath) => {
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None => None
            case Some(existing) => {
              val toUpdate = existing.copy(
                title = newLearningStep.title.map(converterService.asTitle),
                description = newLearningStep.description.map(converterService.asDescription),
                embedUrl = newLearningStep.embedUrl.map(converterService.asEmbedUrl),
                `type` = model.StepType.valueOfOrDefault(newLearningStep.`type`),
                license = newLearningStep.license)

              val updatedStep = learningPathRepository.updateLearningStep(toUpdate)
              val updatedPath = learningPathRepository.update(learningPath.copy(
                learningsteps = learningPath.learningsteps.filterNot(_.id == updatedStep.id) :+ updatedStep,
                lastUpdated = new Date()))

              if (updatedPath.isPublished) {
                searchIndexService.indexLearningPath(updatedPath)
              }

              Some(converterService.asApiLearningStep(updatedStep, updatedPath))
            }
          }
        }
      }
    }

    def deleteLearningStep(learningPathId: Long, learningStepId: Long, owner: String): Boolean = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => false
        case Some(learningPath) => {
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None => false
            case Some(existing) => {
              learningPathRepository.deleteLearningStep(learningPathId, learningStepId)
              val updatedPath = learningPathRepository.update(learningPath.copy(
                learningsteps = learningPath.learningsteps.filterNot(_.id.get == learningStepId),
                lastUpdated = new Date()))

              if (updatedPath.isPublished) {
                searchIndexService.indexLearningPath(updatedPath)
              }
              true
            }
          }
        }
      }
    }

    private def withIdAndAccessGranted(learningPathId: Long, owner: String): Option[model.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.foreach(_.verifyOwner(owner))
      learningPath
    }
  }
}
