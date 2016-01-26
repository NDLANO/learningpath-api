package no.ndla.learningpathapi.service

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.integration.AmazonIntegration
import no.ndla.learningpathapi.service.ModelConverters.{asApiLearningStep, asLearningpath, asLearningpathSummary}


class LearningpathService(publishingStatus: String) {

  val learningpathData = AmazonIntegration.getLearningpathData()

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
    owner match {
      case None => learningpathData.withIdAndStatus(learningPathId.toLong, publishingStatus).map(asLearningpath)
      case Some(o) => learningpathData.withIdStatusAndOwner(learningPathId.toLong, publishingStatus, o).map(asLearningpath)
    }

  }

  def all(owner:Option[String] = None): List[LearningPathSummary] = {
    owner match {
      case None => learningpathData.withStatus(publishingStatus).map(asLearningpathSummary)
      case Some(o) => learningpathData.withStatusAndOwner(publishingStatus, o).map(asLearningpathSummary)
    }
  }
}
