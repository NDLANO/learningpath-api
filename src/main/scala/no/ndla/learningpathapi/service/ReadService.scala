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
import no.ndla.learningpathapi.model.domain.{StepStatus, UserInfo, ValidationException}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent

import scala.math.max
import scala.util.{Failure, Success}

trait ReadService {
  this: LearningPathRepositoryComponent with ConverterService =>
  val readService: ReadService

  class ReadService {

    def tags: List[LearningPathTags] = {
      learningPathRepository.allPublishedTags.map(tags => LearningPathTags(tags.tags, tags.language))
    }

    def contributors: List[Author] = {
      learningPathRepository.allPublishedContributors.map(author => Author(author.`type`, author.name))
    }

    def withOwnerV2(owner: String): List[LearningPathSummaryV2] = {
      learningPathRepository
        .withOwner(owner)
        .flatMap(value => converterService.asApiLearningpathSummaryV2(value).toOption)
    }

    def withIdV2(learningPathId: Long,
                 language: String,
                 fallback: Boolean,
                 user: UserInfo = UserInfo.get): Option[LearningPathV2] = {
      withIdAndAccessGranted(learningPathId, user).flatMap(lp =>
        converterService.asApiLearningpathV2(lp, language, fallback, user))
    }

    def statusFor(learningPathId: Long, user: UserInfo = UserInfo.get): Option[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningStepStatusForV2(learningPathId: Long,
                                learningStepId: Long,
                                language: String,
                                user: UserInfo = UserInfo.get): Option[LearningStepStatus] = {
      learningstepV2For(learningPathId, learningStepId, language, user).map(ls =>
        LearningStepStatus(ls.status.toString))
    }

    def learningstepsForWithStatusV2(learningPathId: Long,
                                     status: StepStatus.Value,
                                     language: String,
                                     fallback: Boolean,
                                     user: UserInfo = UserInfo.get): Option[LearningStepContainerSummary] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) =>
          converterService.asLearningStepContainerSummary(status, lp, language, fallback)
        case None => None
      }
    }

    def learningstepV2For(learningPathId: Long,
                          learningstepId: Long,
                          language: String,
                          user: UserInfo = UserInfo.get): Option[LearningStepV2] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Some(lp) =>
          learningPathRepository
            .learningStepWithId(learningPathId, learningstepId)
            .flatMap(
              ls =>
                converterService
                  .asApiLearningStepV2(ls, lp, language, false, user))
        case None => None
      }
    }

    private def withIdAndAccessGranted(learningPathId: Long, user: UserInfo): Option[domain.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.map(_.isOwnerOrPublic(user)) match {
        case Some(Success(lp)) => Some(lp)
        case Some(Failure(ex)) => throw ex
        case None              => None
      }
    }

    def getLearningPathDomainDump(pageNo: Int, pageSize: Int): LearningPathDomainDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      val results = learningPathRepository.getLearningPathByPage(safePageSize, (safePageNo - 1) * safePageSize)

      LearningPathDomainDump(learningPathRepository.learningPathCount, safePageNo, safePageSize, results)
    }

  }
}
