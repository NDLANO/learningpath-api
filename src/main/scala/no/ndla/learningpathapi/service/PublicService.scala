package no.ndla.learningpathapi.service

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.business.LearningpathData

class PublicService(learningpathData: LearningpathData, mc: ModelConverters) {

  def withId(learningPathId: Long): Option[LearningPath] = {
    withIdAndAccessGranted(learningPathId).map(mc.asApiLearningpath)
  }

  def statusFor(learningPathId: Long): Option[LearningPathStatus] = {
    withIdAndAccessGranted(learningPathId).map(lp => LearningPathStatus(lp.status.toString))
  }

  def learningstepsFor(learningPathId: Long): Option[List[LearningStep]] = {
    withIdAndAccessGranted(learningPathId) match {
      case Some(lp) => Some(learningpathData.learningStepsFor(lp.id.get).map(ls => mc.asApiLearningStep(ls, lp)))
      case None => None
    }
  }

  def learningstepFor(learningPathId: Long, learningstepId: Long): Option[LearningStep] = {
    withIdAndAccessGranted(learningPathId) match {
      case Some(lp) => learningpathData.learningStepWithId(learningPathId, learningstepId).map(ls => mc.asApiLearningStep(ls, lp))
      case None => None
    }
  }

  private def withIdAndAccessGranted(learningPathId: Long): Option[model.LearningPath] = {
    val learningPath = learningpathData.withId(learningPathId)
    learningPath.foreach(_.verifyNotPrivate)
    learningPath
  }
}
