package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.integration.AmazonIntegration
import no.ndla.learningpathapi.service.ModelConverters._


class LearningpathService {

  val learningpathData = AmazonIntegration.getLearningpathData()
  val searchIndex = AmazonIntegration.getLearningPathIndex()

  def all(owner:Option[String] = None, status:String = LearningpathApiProperties.Published): List[LearningPathSummary] = {
    owner match {
      case None => learningpathData.withStatus(status).map(asApiLearningpathSummary)
      case Some(o) => learningpathData.withStatusAndOwner(status, o).map(asApiLearningpathSummary)
    }
  }

  def withId(learningPathId: Long, owner:Option[String] = None): Option[LearningPath] = {
    withIdInternal(learningPathId, owner).map(asApiLearningpath)
  }

  def statusFor(learningPathId: Long, owner:Option[String] = None): Option[LearningPathStatus] = {
    withId(learningPathId, owner).map(lp => LearningPathStatus(lp.status))
  }

  def learningstepsFor(learningPathId: Long, owner:Option[String] = None): Option[List[LearningStep]] = {
    withIdInternal(learningPathId, owner) match {
      case Some(lp) => Some(learningpathData.learningStepsFor(lp.id.get).map(ls => asApiLearningStep(ls, lp)))
      case None => None
    }
  }

  def learningstepFor(learningPathId: Long, learningstepId: Long, owner:Option[String] = None): Option[LearningStep] = {
    withIdInternal(learningPathId, owner) match {
      case Some(lp) => learningpathData.learningStepWithId(learningPathId, learningstepId).map(ls => asApiLearningStep(ls, lp))
      case None => None
    }
  }

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
    withIdInternal(id, Some(owner)) match {
      case None => None
      case Some(existing) => {
        val toUpdate = existing.copy(
          title = newLearningPath.title.map(asTitle),
          description = newLearningPath.description.map(asDescription),
          coverPhotoUrl = newLearningPath.coverPhotoUrl,
          duration = newLearningPath.duration,
          tags = newLearningPath.tags.map(asLearningPathTag),
          lastUpdated = new Date())

        val updatedLearningPath = learningpathData.update(toUpdate)
        if(updatedLearningPath.isPublished) {
          searchIndex.indexLearningPath(updatedLearningPath)
        }

        Some(asApiLearningpath(updatedLearningPath))
      }
    }
  }

  def updateLearningPathStatus(learningPathId: Long, status: LearningPathStatus, owner: String): Option[LearningPath] = {
    status.validate()
    withIdInternal(learningPathId, Some(owner)) match {
      case None => None
      case Some(existing) => {
        val updatedLearningPath = learningpathData.update(
          existing.copy(
            status = status.status,
            lastUpdated = new Date()))

        updatedLearningPath.isPublished match {
          case true => searchIndex.indexLearningPath(updatedLearningPath)
          case false => searchIndex.deleteLearningPath(updatedLearningPath)
        }


        Some(asApiLearningpath(updatedLearningPath))
      }
    }
  }

  def deleteLearningPath(learningPathId: Long, owner: String): Boolean = {
    withIdInternal(learningPathId, Some(owner)) match {
      case None => false
      case Some(existing) => {
        learningpathData.delete(learningPathId)
        searchIndex.deleteLearningPath(existing)
        true
      }
    }
  }

  def addLearningStep(learningPathId: Long, newLearningStep: NewLearningStep, owner: String): Option[LearningStep] = {
    withIdInternal(learningPathId, Some(owner)) match {
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

        val insertedStep = learningpathData.insertLearningStep(newStep)
        val updatedPath = learningpathData.update(learningPath.copy(
          learningsteps = learningPath.learningsteps :+ insertedStep,
          lastUpdated = new Date()))

        if(updatedPath.isPublished){
          searchIndex.indexLearningPath(updatedPath)
        }

        Some(asApiLearningStep(insertedStep, updatedPath))
      }
    }
  }

  def updateLearningStep(learningPathId: Long, learningStepId: Long, newLearningStep: NewLearningStep, owner: String): Option[LearningStep] = {
    withIdInternal(learningPathId, Some(owner)) match {
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
            val updatedPath = learningpathData.update(learningPath.copy(
              learningsteps = learningPath.learningsteps.filterNot(_.id == updatedStep.id) :+ updatedStep,
              lastUpdated = new Date()))

            if(updatedPath.isPublished) {
              searchIndex.indexLearningPath(updatedPath)
            }

            Some(asApiLearningStep(updatedStep, updatedPath))
          }
        }
      }
    }
  }

  def deleteLearningStep(learningPathId: Long, learningStepId: Long, owner: String): Boolean = {
    withIdInternal(learningPathId, Some(owner)) match {
      case None => false
      case Some(existing) => {
        learningpathData.deleteLearningStep(learningStepId)
        val updatedPath = learningpathData.update(existing.copy(
          learningsteps = existing.learningsteps.filterNot(_.id.get == learningStepId),
          lastUpdated = new Date()))

        if(updatedPath.isPublished) {
          searchIndex.indexLearningPath(updatedPath)
        }

        true
      }
    }
  }

  private def withIdInternal(learningPathId: Long, owner:Option[String] = None): Option[model.LearningPath] = {
    val learningPath = learningpathData.withId(learningPathId)
    learningPath.foreach(_.verifyAccess(owner))
    learningPath
  }
}
