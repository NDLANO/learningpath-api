package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent


trait ReadServiceComponent {
  this: LearningPathRepositoryComponent with ConverterServiceComponent =>
  val readService: ReadService

  class ReadService {

    def tags: List[LearningPathTag] = {
      learningPathRepository.allPublishedTags.map(tag => LearningPathTag(tag.tag, tag.language))
    }

    def withOwner(owner: String): List[LearningPathSummary] = {
      learningPathRepository.withOwner(owner).map(converterService.asApiLearningpathSummary)
    }

    def withId(learningPathId: Long, user: Option[String] = None): Option[LearningPath] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => converterService.asApiLearningpath(lp, user))
    }

    def statusFor(learningPathId: Long, user: Option[String] = None): Option[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningstepsFor(learningPathId: Long, user: Option[String] = None): Option[List[LearningStepSummary]] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) => Some(learningPathRepository.learningStepsFor(lp.id.get).map(ls => converterService.asApiLearningStepSummary(ls, lp)).sortBy(_.seqNo))
        case None => None
      }
    }

    def learningstepFor(learningPathId: Long, learningstepId: Long, user: Option[String] = None): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) => learningPathRepository.learningStepWithId(learningPathId, learningstepId).map(ls => converterService.asApiLearningStep(ls, lp, user))
        case None => None
      }
    }

    private def withIdAndAccessGranted(learningPathId: Long, user: Option[String]): Option[domain.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.foreach(_.verifyOwnerOrPublic(user))
      learningPath
    }
  }
}
