package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.integration.AmazonIntegration
import no.ndla.learningpathapi.service.ModelConverters._


class LearningpathService(publishingStatus: String) {
  val learningpathData = AmazonIntegration.getLearningpathData()

  def updateLearningPathStatus(learningPathId: String, status: LearningPathStatus, owner: String): Option[LearningPath] = {
    status.validate()

    withIdInternal(learningPathId, Some(owner)) match {
      case None => None
      case Some(existing) => {
        val learningPath = model.LearningPath(
          existing.id,
          existing.title,
          existing.description,
          existing.learningsteps,
          existing.coverPhotoUrl,
          existing.duration,
          status.status,
          existing.verificationStatus,
          new Date(), existing.tags, owner)

        Some(asApiLearningpath(learningpathData.update(learningPath)))
      }
    }
  }

  def updateLearningStep(learningPathId: String, learningStepId: String, newLearningStep: NewLearningStep, owner: String): Option[LearningStep] = {
    withIdInternal(learningPathId, Some(owner)) match {
      case None => None
      case Some(learningPath) => {
        learningPath.learningsteps.find(ls => ls.id == learningStepId.toLong) match {
          case None => None
          case Some(existingStep) => {
            val updatedStep = model.LearningStep(
              existingStep.id,
              existingStep.seqNo,
              newLearningStep.title.map(asTitle),
              newLearningStep.description.map(asDescription),
              newLearningStep.embedUrl.map(asEmbedUrl),
              newLearningStep.`type`,
              newLearningStep.license)

            val toUpdate = model.LearningPath(
              learningPath.id,
              learningPath.title,
              learningPath.description,
              (updatedStep :: learningPath.learningsteps.filterNot(_.id == existingStep.id)).reverse,
              learningPath.coverPhotoUrl,
              learningPath.duration,
              learningPath.status,
              learningPath.verificationStatus,
              new Date(), learningPath.tags, learningPath.owner)

            Some(asApiLearningStep(updatedStep, learningpathData.update(toUpdate)))
          }
        }
      }
    }
  }

  // TODO: Mye og rotete kode. Vurder en annen strategi for id-er.
  // TODO: Må sjekke på seqNo?
  def addLearningStep(learningPathId: String, newLearningStep: NewLearningStep, owner: String): Option[LearningStep] = {
    withIdInternal(learningPathId, Some(owner)) match {
      case None => None
      case Some(learningPath) => {
        val newId = learningPath.learningsteps.isEmpty match {
          case true => 1
          case false => learningPath.learningsteps.map(_.id).max + 1
        }

        val newSeqNo = learningPath.learningsteps.isEmpty match {
          case true => 1
          case false => learningPath.learningsteps.map(_.seqNo).max + 1
        }

        val newStep = model.LearningStep(
          newId,
          newSeqNo,
          newLearningStep.title.map(asTitle),
          newLearningStep.description.map(asDescription),
          newLearningStep.embedUrl.map(asEmbedUrl),
          newLearningStep.`type`,
          newLearningStep.license)

        val toUpdate = model.LearningPath(
          learningPath.id,
          learningPath.title,
          learningPath.description,
          (newStep :: learningPath.learningsteps).reverse,
          learningPath.coverPhotoUrl,
          learningPath.duration,
          learningPath.status,
          learningPath.verificationStatus,
          new Date(), learningPath.tags, learningPath.owner)

        Some(asApiLearningStep(newStep, learningpathData.update(toUpdate)))
      }
    }
  }

  def updateLearningPath(id:String, newLearningPath: NewLearningPath, owner: String): Option[LearningPath] = {
    withIdInternal(id, Some(owner)) match {
      case None => None
      case Some(existing) => {
        val learningPath = model.LearningPath(
          existing.id,
          newLearningPath.title.map(asTitle),
          newLearningPath.description.map(asDescription),
          existing.learningsteps,
          newLearningPath.coverPhotoUrl,
          newLearningPath.duration,
          existing.status,
          existing.verificationStatus,
          new Date(), newLearningPath.tags.map(asLearningPathTag), owner)

        Some(asApiLearningpath(learningpathData.update(learningPath)))
      }
    }
  }

  def addLearningPath(newLearningPath: NewLearningPath, owner:String): LearningPath = {
    val learningPath = model.LearningPath(None,
      newLearningPath.title.map(asTitle),
      newLearningPath.description.map(asDescription),
      List(),
      newLearningPath.coverPhotoUrl,
      newLearningPath.duration, publishingStatus,
      LearningpathApiProperties.External, // TODO: Regler for å sette disse
      new Date(), newLearningPath.tags.map(asLearningPathTag), owner)

    asApiLearningpath(learningpathData.insert(learningPath))
  }

  def learningstepFor(learningpathId: String, learningstepId: String, owner:Option[String] = None): Option[LearningStep] = {
    learningstepsFor(learningpathId, owner) match {
      case Some(x) => x.find(_.id == learningstepId.toLong)
      case None => None
    }
  }

  def learningstepsFor(learningPathId: String, owner:Option[String] = None): Option[List[LearningStep]] = {
    val learningPath = owner match {
      case None => learningpathData.withIdAndStatus(learningPathId.toLong, publishingStatus)
      case Some(o) => learningpathData.withIdStatusAndOwner(learningPathId.toLong, publishingStatus, o)
    }

    learningPath match {
      case Some(lp) => Some(lp.learningsteps.map(ls => asApiLearningStep(ls, lp)))
      case None => None
    }
  }

  def statusFor(learningPathId: String, owner:Option[String] = None): Option[LearningPathStatus] = {
    owner match {
      case None => learningpathData.withIdAndStatus(learningPathId.toLong, publishingStatus).map(learningpath => LearningPathStatus(learningpath.status))
      case Some(o) => learningpathData.withIdStatusAndOwner(learningPathId.toLong, publishingStatus, o).map(learningpath => LearningPathStatus(learningpath.status))
    }

  }

  def withId(learningPathId: String, owner:Option[String] = None): Option[LearningPath] = {
    withIdInternal(learningPathId, owner).map(asApiLearningpath)
  }

  private def withIdInternal(learningPathId: String, owner:Option[String] = None): Option[model.LearningPath] = {
    owner match {
      case None => learningpathData.withId(learningPathId.toLong)
      case Some(o) => learningpathData.withIdAndOwner(learningPathId.toLong, o)
    }
  }

  def all(owner:Option[String] = None): List[LearningPathSummary] = {
    owner match {
      case None => learningpathData.withStatus(publishingStatus).map(asApiLearningpathSummary)
      case Some(o) => learningpathData.withStatusAndOwner(publishingStatus, o).map(asApiLearningpathSummary)
    }
  }
}
