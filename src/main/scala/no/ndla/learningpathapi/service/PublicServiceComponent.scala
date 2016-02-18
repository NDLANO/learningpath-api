package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.ModelConverters._
import no.ndla.learningpathapi.{LearningPath, LearningPathStatus, LearningStep, model}


trait PublicServiceComponent {
  this: LearningPathRepositoryComponent =>
  val publicService: PublicService

  class PublicService {
    def withId(learningPathId: Long): Option[LearningPath] = {
      withIdAndAccessGranted(learningPathId).map(asApiLearningpath)
    }

    def statusFor(learningPathId: Long): Option[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningstepsFor(learningPathId: Long): Option[List[LearningStep]] = {
      withIdAndAccessGranted(learningPathId) match {
        case Some(lp) => Some(learningPathRepository.learningStepsFor(lp.id.get).map(ls => asApiLearningStep(ls, lp)))
        case None => None
      }
    }

    def learningstepFor(learningPathId: Long, learningstepId: Long): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId) match {
        case Some(lp) => learningPathRepository.learningStepWithId(learningPathId, learningstepId).map(ls => asApiLearningStep(ls, lp))
        case None => None
      }
    }

    private def withIdAndAccessGranted(learningPathId: Long): Option[model.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.foreach(_.verifyNotPrivate)
      learningPath
    }
  }
}
