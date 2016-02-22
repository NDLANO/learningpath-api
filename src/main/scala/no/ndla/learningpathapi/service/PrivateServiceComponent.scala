package no.ndla.learningpathapi.service

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent

trait PrivateServiceComponent {
  this: LearningPathRepositoryComponent with ConverterServiceComponent =>
  val privateService: PrivateService

  class PrivateService {
    def all(owner: String): List[LearningPathSummary] = {
      learningPathRepository.withStatusAndOwner(model.LearningPathStatus.PRIVATE, owner).map(converterService.asApiLearningpathSummary)
    }

    def withId(learningPathId: Long, owner: String): Option[LearningPath] = {
      withIdAndAccessGranted(learningPathId, owner).map(converterService.asApiLearningpath)
    }

    def statusFor(learningPathId: Long, owner: String): Option[LearningPathStatus] = {
      withId(learningPathId, owner).map(lp => LearningPathStatus(lp.status))
    }

    def learningstepsFor(learningPathId: Long, owner: String): Option[List[LearningStep]] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case Some(lp) => Some(learningPathRepository.learningStepsFor(lp.id.get).map(ls => converterService.asApiLearningStep(ls, lp)))
        case None => None
      }
    }

    def learningstepFor(learningPathId: Long, learningStepId: Long, owner: String): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case Some(lp) => learningPathRepository.learningStepWithId(learningPathId, learningStepId).map(ls => converterService.asApiLearningStep(ls, lp))
        case None => None
      }
    }

    private def withIdAndAccessGranted(learningPathId: Long, owner: String): Option[model.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.foreach(_.verifyOwner(owner))
      learningPath
    }
  }
}
