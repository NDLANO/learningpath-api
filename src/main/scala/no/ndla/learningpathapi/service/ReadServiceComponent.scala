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
import no.ndla.learningpathapi.model.domain.{StepStatus, ValidationException}
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

    def withOwnerV2(owner: String, language: String): List[LearningPathSummaryV2] = {
      learningPathRepository.withOwner(owner).flatMap(value => converterService.asApiLearningpathSummaryV2(value, language).toOption)
    }

    def withIdV2(learningPathId: Long, language: String, user: Option[String] = None): Option[LearningPathV2] = {
      withIdAndAccessGranted(learningPathId, user).flatMap(lp => converterService.asApiLearningpathV2(lp, language, user))
    }

    def statusFor(learningPathId: Long, user: Option[String] = None): Option[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningStepStatusForV2(learningPathId: Long, learningStepId: Long, language: String, user: Option[String] = None): Option[LearningStepStatus] = {
      learningstepV2For(learningPathId, learningStepId, language, user).map(ls => LearningStepStatus(ls.status.toString))
    }

    def learningstepsForWithStatusV2(learningPathId: Long, status: StepStatus.Value, language: String, user: Option[String] = None): Option[LearningStepContainerSummary] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) => converterService.asLearningStepContainerSummary(status, lp, language)
        case None => None
      }
    }

    def learningstepV2For(learningPathId: Long, learningstepId: Long, language: String, user: Option[String] = None): Option[LearningStepV2] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) =>
          learningPathRepository.learningStepWithId(learningPathId, learningstepId)
            .flatMap(ls => converterService.asApiLearningStepV2(ls, lp, language, user))
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
