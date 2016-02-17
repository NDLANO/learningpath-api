package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.business.{LearningPathIndex, LearningpathData}
import no.ndla.learningpathapi.model.LearningPathVerificationStatus

class UpdateService(learningpathData: LearningpathData, searchIndex: LearningPathIndex, mc: ModelConverters) {

  def addLearningPath(newLearningPath: NewLearningPath, owner: String): LearningPath = {
    val learningPath = model.LearningPath(None,None,
      newLearningPath.title.map(mc.asTitle),
      newLearningPath.description.map(mc.asDescription),
      newLearningPath.coverPhotoUrl,
      newLearningPath.duration, model.LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.EXTERNAL,
      new Date(), newLearningPath.tags.map(mc.asLearningPathTag), owner, List())

    mc.asApiLearningpath(learningpathData.insert(learningPath))
  }

  def updateLearningPath(id: Long, newLearningPath: NewLearningPath, owner: String): Option[LearningPath] = {
    withIdAndAccessGranted(id, owner) match {
      case None => None
      case Some(existing) => {
        val toUpdate = existing.copy(
          title = newLearningPath.title.map(mc.asTitle),
          description = newLearningPath.description.map(mc.asDescription),
          coverPhotoUrl = newLearningPath.coverPhotoUrl,
          duration = newLearningPath.duration,
          tags = newLearningPath.tags.map(mc.asLearningPathTag),
          lastUpdated = new Date())

        val updatedLearningPath = learningpathData.update(toUpdate)
        if (updatedLearningPath.isPublished) {
          searchIndex.indexLearningPath(updatedLearningPath)
        }

        Some(mc.asApiLearningpath(updatedLearningPath))
      }
    }
  }

  def updateLearningPathStatus(learningPathId: Long, status: LearningPathStatus, owner: String): Option[LearningPath] = {
    status.validate()
    withIdAndAccessGranted(learningPathId, owner) match {
      case None => None
      case Some(existing) => {
        val updatedLearningPath = learningpathData.update(
          existing.copy(
            status = model.LearningPathStatus.valueOfOrDefault(status.status),
            lastUpdated = new Date()))

        updatedLearningPath.isPublished match {
          case true => searchIndex.indexLearningPath(updatedLearningPath)
          case false => searchIndex.deleteLearningPath(updatedLearningPath)
        }


        Some(mc.asApiLearningpath(updatedLearningPath))
      }
    }
  }

  def deleteLearningPath(learningPathId: Long, owner: String): Boolean = {
    withIdAndAccessGranted(learningPathId, owner) match {
      case None => false
      case Some(existing) => {
        learningpathData.delete(learningPathId)
        searchIndex.deleteLearningPath(existing)
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
          newLearningStep.title.map(mc.asTitle),
          newLearningStep.description.map(mc.asDescription),
          newLearningStep.embedUrl.map(mc.asEmbedUrl),
          model.StepType.valueOfOrDefault(newLearningStep.`type`),
          newLearningStep.license)

        val insertedStep = learningpathData.insertLearningStep(newStep)
        val updatedPath = learningpathData.update(learningPath.copy(
          learningsteps = learningPath.learningsteps :+ insertedStep,
          lastUpdated = new Date()))

        if (updatedPath.isPublished) {
          searchIndex.indexLearningPath(updatedPath)
        }

        Some(mc.asApiLearningStep(insertedStep, updatedPath))
      }
    }
  }

  def updateLearningStep(learningPathId: Long, learningStepId: Long, newLearningStep: NewLearningStep, owner: String): Option[LearningStep] = {
    withIdAndAccessGranted(learningPathId, owner) match {
      case None => None
      case Some(learningPath) => {
        learningpathData.learningStepWithId(learningPathId, learningStepId) match {
          case None => None
          case Some(existing) => {
            val toUpdate = existing.copy(
              title = newLearningStep.title.map(mc.asTitle),
              description = newLearningStep.description.map(mc.asDescription),
              embedUrl = newLearningStep.embedUrl.map(mc.asEmbedUrl),
              `type` = model.StepType.valueOfOrDefault(newLearningStep.`type`),
              license = newLearningStep.license)

            val updatedStep = learningpathData.updateLearningStep(toUpdate)
            val updatedPath = learningpathData.update(learningPath.copy(
              learningsteps = learningPath.learningsteps.filterNot(_.id == updatedStep.id) :+ updatedStep,
              lastUpdated = new Date()))

            if (updatedPath.isPublished) {
              searchIndex.indexLearningPath(updatedPath)
            }

            Some(mc.asApiLearningStep(updatedStep, updatedPath))
          }
        }
      }
    }
  }

  def deleteLearningStep(learningPathId: Long, learningStepId: Long, owner: String): Boolean = {
    withIdAndAccessGranted(learningPathId, owner) match {
      case None => false
      case Some(learningPath) => {
        learningpathData.learningStepWithId(learningPathId, learningStepId) match {
          case None => false
          case Some(existing) => {
            learningpathData.deleteLearningStep(learningStepId)
            val updatedPath = learningpathData.update(learningPath.copy(
              learningsteps = learningPath.learningsteps.filterNot(_.id.get == learningStepId),
              lastUpdated = new Date()))

            if (updatedPath.isPublished) {
              searchIndex.indexLearningPath(updatedPath)
            }
            true
          }
        }
      }
    }
  }

  private def withIdAndAccessGranted(learningPathId: Long, owner: String): Option[model.LearningPath] = {
    val learningPath = learningpathData.withId(learningPathId)
    learningPath.foreach(_.verifyOwner(owner))
    learningPath
  }
}
