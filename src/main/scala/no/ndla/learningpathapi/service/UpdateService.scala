package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.business.LearningpathData
import no.ndla.learningpathapi.service.ModelConverters._

class UpdateService(learningpathData: LearningpathData) {

  def addLearningPath(newLearningPath: NewLearningPath, owner:String): LearningPath = {
    val learningPath = model.LearningPath(None,
      newLearningPath.title.map(asTitle),
      newLearningPath.description.map(asDescription),
      newLearningPath.coverPhotoUrl,
      newLearningPath.duration, LearningpathApiProperties.Private,
      LearningpathApiProperties.External,
      new Date(), newLearningPath.tags.map(asLearningPathTag), owner, List())

    asApiLearningpath(learningpathData.insert(learningPath))
  }

  def updateLearningPath(id:Long, newLearningPath: NewLearningPath, owner: String): Option[LearningPath] = {
    withIdAndAccessGranted(id, owner) match {
      case None => None
      case Some(existing) => {
        val learningPath = model.LearningPath(
          existing.id,
          newLearningPath.title.map(asTitle),
          newLearningPath.description.map(asDescription),
          newLearningPath.coverPhotoUrl,
          newLearningPath.duration,
          existing.status,
          existing.verificationStatus,
          new Date(), newLearningPath.tags.map(asLearningPathTag), owner, existing.learningsteps)

        Some(asApiLearningpath(learningpathData.update(learningPath)))
      }
    }
  }

  def updateLearningPathStatus(learningPathId: Long, status: LearningPathStatus, owner: String): Option[LearningPath] = {
    status.validate()
    withIdAndAccessGranted(learningPathId, owner) match {
      case None => None
      case Some(existing) => {
        val learningPath = model.LearningPath(
          existing.id,
          existing.title,
          existing.description,
          existing.coverPhotoUrl,
          existing.duration,
          status.status,
          existing.verificationStatus,
          new Date(), existing.tags, owner, existing.learningsteps)

        Some(asApiLearningpath(learningpathData.update(learningPath)))
      }
    }
  }

  def deleteLearningPath(learningPathId: Long, owner: String): Boolean = {
    withIdAndAccessGranted(learningPathId, owner) match {
      case None => false
      case Some(existing) => {
        learningpathData.delete(learningPathId)
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
          learningPath.id,
          newSeqNo,
          newLearningStep.title.map(asTitle),
          newLearningStep.description.map(asDescription),
          newLearningStep.embedUrl.map(asEmbedUrl),
          newLearningStep.`type`,
          newLearningStep.license)

        val inserted = learningpathData.insertLearningStep(newStep)
        val updated = learningpathData.update(learningPath.copy(lastUpdated = new Date()))
        Some(asApiLearningStep(inserted, updated))
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
              title = newLearningStep.title.map(asTitle),
              description = newLearningStep.description.map(asDescription),
              embedUrl = newLearningStep.embedUrl.map(asEmbedUrl),
              `type` = newLearningStep.`type`,
              license = newLearningStep.license)

            val updatedStep = learningpathData.updateLearningStep(toUpdate)
            val updatedPath = learningpathData.update(learningPath.copy(lastUpdated = new Date()))
            Some(asApiLearningStep(updatedStep, updatedPath))
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
            learningpathData.update(learningPath.copy(lastUpdated = new Date()))
            true
          }
        }
      }
    }
  }

  private def withIdAndAccessGranted(learningPathId: Long, owner:String): Option[model.LearningPath] = {
    val learningPath = learningpathData.withId(learningPathId)
    learningPath.foreach(_.verifyOwner(owner))
    learningPath
  }
}
