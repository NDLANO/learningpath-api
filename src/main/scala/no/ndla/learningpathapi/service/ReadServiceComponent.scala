package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi._


trait ReadServiceComponent {
  this: LearningPathRepositoryComponent with ConverterServiceComponent =>
  val readService: ReadService

  class ReadService {
    def withOwner(owner: String): List[LearningPathSummary] = {
      learningPathRepository.withOwner(owner).map(converterService.asApiLearningpathSummary)
    }

    def withId(learningPathId: Long, user: Option[String] = None): Option[LearningPath] = {
      withIdAndAccessGranted(learningPathId, user).map(converterService.asApiLearningpath)
    }

    def statusFor(learningPathId: Long, user: Option[String] = None): Option[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningstepsFor(learningPathId: Long, user: Option[String] = None): Option[List[LearningStep]] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) => Some(learningPathRepository.learningStepsFor(lp.id.get).map(ls => converterService.asApiLearningStep(ls, lp)).sortBy(_.seqNo))
        case None => None
      }
    }


    def learningstepFor(learningPathId: Long, learningstepId: Long, user: Option[String] = None): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) => learningPathRepository.learningStepWithId(learningPathId, learningstepId).map(ls => converterService.asApiLearningStep(ls, lp))
        case None => None
      }
    }

    private def withIdAndAccessGranted(learningPathId: Long, user: Option[String]): Option[model.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.foreach(_.verifyOwnerOrPublic(user))
      learningPath
    }
  }
}
