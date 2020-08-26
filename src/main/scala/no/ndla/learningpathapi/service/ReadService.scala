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
import no.ndla.learningpathapi.model.domain.config.ConfigKey
import no.ndla.learningpathapi.model.domain.{
  StepStatus,
  UserInfo,
  Author => _,
  LearningPathStatus => _,
  LearningPathTags => _,
  _
}
import no.ndla.learningpathapi.repository.{ConfigRepository, LearningPathRepositoryComponent}

import scala.math.max
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: LearningPathRepositoryComponent with ConfigRepository with ConverterService =>
  val readService: ReadService

  class ReadService {

    def tags: List[LearningPathTags] = {
      learningPathRepository.allPublishedTags.map(tags => LearningPathTags(tags.tags, tags.language))
    }

    def contributors: List[Author] = {
      learningPathRepository.allPublishedContributors.map(author => Author(author.`type`, author.name))
    }

    def withOwnerV2(user: UserInfo = UserInfo.getUserOrPublic): List[LearningPathSummaryV2] = {
      learningPathRepository
        .withOwner(user.userId)
        .flatMap(value => converterService.asApiLearningpathSummaryV2(value, user).toOption)
    }

    def withIdV2(learningPathId: Long,
                 language: String,
                 fallback: Boolean,
                 user: UserInfo = UserInfo.getUserOrPublic): Try[LearningPathV2] = {
      withIdAndAccessGranted(learningPathId, user).flatMap(lp =>
        converterService.asApiLearningpathV2(lp, language, fallback, user))
    }

    def statusFor(learningPathId: Long, user: UserInfo = UserInfo.getUserOrPublic): Try[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningStepStatusForV2(learningPathId: Long,
                                learningStepId: Long,
                                language: String,
                                fallback: Boolean,
                                user: UserInfo = UserInfo.getUserOrPublic): Try[LearningStepStatus] = {
      learningstepV2For(learningPathId, learningStepId, language, fallback, user).map(ls =>
        LearningStepStatus(ls.status.toString))
    }

    def learningstepsForWithStatusV2(learningPathId: Long,
                                     status: StepStatus.Value,
                                     language: String,
                                     fallback: Boolean,
                                     user: UserInfo = UserInfo.getUserOrPublic): Try[LearningStepContainerSummary] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Success(lp) => converterService.asLearningStepContainerSummary(status, lp, language, fallback)
        case Failure(ex) => Failure(ex)
      }
    }

    def learningstepV2For(learningPathId: Long,
                          learningStepId: Long,
                          language: String,
                          fallback: Boolean,
                          user: UserInfo = UserInfo.getUserOrPublic): Try[LearningStepV2] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Success(lp) =>
          learningPathRepository
            .learningStepWithId(learningPathId, learningStepId)
            .map(ls => converterService.asApiLearningStepV2(ls, lp, language, fallback, user)) match {
            case Some(value) => value
            case None =>
              Failure(
                NotFoundException(
                  s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"))
          }
        case Failure(ex) => Failure(ex)
      }
    }

    private def withIdAndAccessGranted(learningPathId: Long, user: UserInfo): Try[domain.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.map(_.isOwnerOrPublic(user)) match {
        case Some(Success(lp)) => Success(lp)
        case Some(Failure(ex)) => Failure(ex)
        case None              => Failure(NotFoundException(s"Could not find learningPath with id $learningPathId"))
      }
    }

    def getLearningPathDomainDump(pageNo: Int, pageSize: Int, onlyIncludePublished: Boolean): LearningPathDomainDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))

      val resultFunc =
        if (onlyIncludePublished) learningPathRepository.getPublishedLearningPathByPage _
        else learningPathRepository.getAllLearningPathsByPage _

      val count =
        if (onlyIncludePublished) learningPathRepository.publishedLearningPathCount
        else learningPathRepository.learningPathCount

      val results = resultFunc(safePageSize, (safePageNo - 1) * safePageSize)

      LearningPathDomainDump(count, safePageNo, safePageSize, results)
    }

    def learningPathWithStatus(status: String, user: UserInfo): Try[List[LearningPathV2]] = {
      if (user.isAdmin) {
        domain.LearningPathStatus.valueOf(status) match {
          case Some(ps) =>
            Success(
              learningPathRepository
                .learningPathsWithStatus(ps)
                .flatMap(lp => converterService.asApiLearningpathV2(lp, "all", fallback = true, user).toOption))
          case _ => Failure(InvalidStatusException(s"Parameter '$status' is not a valid status"))
        }
      } else { Failure(AccessDeniedException("You do not have access to this resource.")) }
    }

    def isWriteRestricted: Boolean =
      Try(
        configRepository
          .getConfigWithKey(ConfigKey.IsWriteRestricted)
          .map(_.value.toBoolean)
      ).toOption.flatten.getOrElse(false)

    def canWriteNow(userInfo: UserInfo): Boolean =
      userInfo.canWriteDuringWriteRestriction || !readService.isWriteRestricted
  }
}
