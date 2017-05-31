/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.StepStatus
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent


trait ReadServiceComponent {
  this: LearningPathRepositoryComponent with ConverterServiceComponent =>
  val readService: ReadService

  class ReadService {


    def tags: List[LearningPathTags] = {
      learningPathRepository.allPublishedTags.map(tags => LearningPathTags(tags.tags, tags.language))
    }

    def contributors: List[Author] = {
      learningPathRepository.allPublishedContributors.map(author => Author(author.`type`, author.name))
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

    def learningStepStatusFor(learningPathId: Long, learningStepId: Long, user: Option[String] = None): Option[LearningStepStatus] = {
      learningstepFor(learningPathId, learningStepId, user).map(ls => LearningStepStatus(ls.status.toString))
    }

    def learningstepsForWithStatus(learningPathId: Long, status: StepStatus.Value, user: Option[String] = None): Option[Seq[LearningStepSummary]] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) => Some(
          learningPathRepository.learningStepsFor(lp.id.get)
            .filter(_.status == status)
            .map(ls => converterService.asApiLearningStepSummary(ls, lp)).sortBy(_.seqNo))

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
