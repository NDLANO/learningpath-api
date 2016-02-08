package no.ndla.learningpathapi.service

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.business.LearningpathData
import no.ndla.learningpathapi.service.ModelConverters._

class PrivateService(learningpathData: LearningpathData) {

  def all(owner: String): List[LearningPathSummary] = {
    learningpathData.withStatusAndOwner(LearningpathApiProperties.Private, owner).map(asApiLearningpathSummary)
  }

  def withId(learningPathId: Long, owner: String): Option[LearningPath] = {
    withIdAndAccessGranted(learningPathId, owner).map(asApiLearningpath)
  }

  def statusFor(learningPathId: Long, owner: String): Option[LearningPathStatus] = {
    withId(learningPathId, owner).map(lp => LearningPathStatus(lp.status))
  }

  def learningstepsFor(learningPathId: Long, owner: String): Option[List[LearningStep]] = {
    withIdAndAccessGranted(learningPathId, owner) match {
      case Some(lp) => Some(learningpathData.learningStepsFor(lp.id.get).map(ls => asApiLearningStep(ls, lp)))
      case None => None
    }
  }

  def learningstepFor(learningPathId: Long, learningStepId: Long, owner: String): Option[LearningStep] = {
    withIdAndAccessGranted(learningPathId, owner) match {
      case Some(lp) => learningpathData.learningStepWithId(learningPathId, learningStepId).map(ls => asApiLearningStep(ls, lp))
      case None => None
    }
  }

  private def withIdAndAccessGranted(learningPathId: Long, owner: String): Option[model.LearningPath] = {
    val learningPath = learningpathData.withId(learningPathId)
    learningPath.foreach(_.verifyOwner(owner))
    learningPath
  }
}
