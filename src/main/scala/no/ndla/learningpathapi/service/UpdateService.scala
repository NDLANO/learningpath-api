/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.api.config.UpdateConfigValue
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigValue}
import no.ndla.learningpathapi.model.domain.{LearningPathStatus, UserInfo, LearningPath => _, LearningStep => _, _}
import no.ndla.learningpathapi.repository.{ConfigRepository, LearningPathRepositoryComponent}
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.learningpathapi.validation.{LearningPathValidator, LearningStepValidator}

import scala.util.{Failure, Success}

trait UpdateService {
  this: LearningPathRepositoryComponent
    with ConfigRepository
    with ConverterService
    with SearchIndexService
    with Clock
    with LearningStepValidator
    with LearningPathValidator =>
  val updateService: UpdateService

  class UpdateService {

    def newFromExistingV2(id: Long, newLearningPath: NewCopyLearningPathV2, owner: UserInfo): Option[LearningPathV2] = {
      learningPathRepository.withId(id).map(_.isOwnerOrPublic(owner)) match {
        case None              => None
        case Some(Failure(ex)) => throw ex
        case Some(Success(existing)) =>
          val toInsert = converterService.newFromExistingLearningPath(existing, newLearningPath, owner)
          learningPathValidator.validate(toInsert, allowUnknownLanguage = true)
          converterService.asApiLearningpathV2(learningPathRepository.insert(toInsert),
                                               newLearningPath.language,
                                               true,
                                               owner)
      }
    }

    def addLearningPathV2(newLearningPath: NewLearningPathV2, owner: UserInfo): Option[LearningPathV2] = {
      val learningPath = converterService.newLearningPath(newLearningPath, owner)
      learningPathValidator.validate(learningPath)

      converterService.asApiLearningpathV2(learningPathRepository.insert(learningPath),
                                           newLearningPath.language,
                                           true,
                                           owner)
    }

    def updateLearningPathV2(id: Long,
                             learningPathToUpdate: UpdatedLearningPathV2,
                             owner: UserInfo): Option[LearningPathV2] = {
      // Should not be able to submit with an illegal language
      learningPathValidator.validate(learningPathToUpdate)

      withId(id).map(_.canEditLearningpath(owner)) match {
        case Some(Failure(ex)) => throw ex
        case None              => None
        case Some(Success(existing)) =>
          val toUpdate = converterService.mergeLearningPaths(existing, learningPathToUpdate, owner)

          // Imported learningpaths may contain fields with language=unknown.
          // We should still be able to update it, but not add new fields with language=unknown.
          learningPathValidator.validate(toUpdate, allowUnknownLanguage = true)

          val updatedLearningPath = learningPathRepository.update(toUpdate)
          if (updatedLearningPath.isPublished) {
            searchIndexService.indexDocument(updatedLearningPath)
          } else {
            deleteIsBasedOnReference(existing)
            searchIndexService.deleteDocument(existing)
          }

          converterService.asApiLearningpathV2(updatedLearningPath, learningPathToUpdate.language, true, owner)
      }
    }

    def updateLearningPathStatusV2(learningPathId: Long,
                                   status: LearningPathStatus.Value,
                                   owner: UserInfo,
                                   language: String,
                                   message: Option[String] = None): Option[LearningPathV2] = {
      withId(learningPathId, includeDeleted = true).map(_.canSetStatus(status, owner)) match {
        case Some(Failure(ex)) => throw ex
        case None              => None
        case Some(Success(existing)) =>
          if (status == domain.LearningPathStatus.PUBLISHED) {
            existing.validateForPublishing()
          }

          val newMessage = message match {
            case Some(msg) if owner.isAdmin =>
              Some(domain.Message(msg, owner.userId, clock.now()))
            case _ => existing.message
          }

          val updatedLearningPath =
            learningPathRepository.update(
              existing.copy(message = newMessage, status = status, lastUpdated = clock.now()))

          if (updatedLearningPath.isPublished) {
            searchIndexService.indexDocument(updatedLearningPath)
          } else {
            deleteIsBasedOnReference(updatedLearningPath)
            searchIndexService.deleteDocument(updatedLearningPath)
          }

          converterService.asApiLearningpathV2(updatedLearningPath, language, true, owner)
      }
    }

    private[service] def deleteIsBasedOnReference(updatedLearningPath: domain.LearningPath): Unit = {
      learningPathRepository
        .learningPathsWithIsBasedOn(updatedLearningPath.id.get)
        .foreach(lp => {
          learningPathRepository.update(
            lp.copy(
              lastUpdated = clock.now(),
              isBasedOn = None
            )
          )
        })
    }

    def addLearningStepV2(learningPathId: Long,
                          newLearningStep: NewLearningStepV2,
                          owner: UserInfo): Option[LearningStepV2] = {
      optimisticLockRetries(10) {
        withId(learningPathId).map(_.canEditLearningpath(owner)) match {
          case None              => None
          case Some(Failure(ex)) => throw ex
          case Some(Success(learningPath)) =>
            val newStep = converterService.asDomainLearningStep(newLearningStep, learningPath)
            learningStepValidator.validate(newStep)

            val (insertedStep, updatedPath) = inTransaction { implicit session =>
              val insertedStep =
                learningPathRepository.insertLearningStep(newStep)
              val toUpdate = converterService.insertLearningStep(learningPath, insertedStep, owner)
              val updatedPath = learningPathRepository.update(toUpdate)

              (insertedStep, updatedPath)
            }
            if (updatedPath.isPublished) {
              searchIndexService.indexDocument(updatedPath)
            } else {
              deleteIsBasedOnReference(learningPath)
              searchIndexService.deleteDocument(learningPath)
            }
            converterService.asApiLearningStepV2(insertedStep, updatedPath, newLearningStep.language, true, owner)
        }
      }
    }

    def updateLearningStepV2(learningPathId: Long,
                             learningStepId: Long,
                             learningStepToUpdate: UpdatedLearningStepV2,
                             owner: UserInfo): Option[LearningStepV2] = {
      withId(learningPathId).map(_.canEditLearningpath(owner)) match {
        case None              => None
        case Some(Failure(ex)) => throw ex
        case Some(Success(learningPath)) =>
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None => None
            case Some(existing) =>
              val toUpdate = converterService.mergeLearningSteps(existing, learningStepToUpdate)
              learningStepValidator.validate(toUpdate)

              val (updatedStep, updatedPath) = inTransaction { implicit session =>
                val updatedStep =
                  learningPathRepository.updateLearningStep(toUpdate)
                val pathToUpdate = converterService.insertLearningStep(learningPath, updatedStep, owner)
                val updatedPath = learningPathRepository.update(pathToUpdate)

                (updatedStep, updatedPath)
              }

              if (updatedPath.isPublished) {
                searchIndexService.indexDocument(updatedPath)
              } else {
                deleteIsBasedOnReference(updatedPath)
                searchIndexService.deleteDocument(updatedPath)
              }

              converterService.asApiLearningStepV2(updatedStep, updatedPath, learningStepToUpdate.language, true, owner)
          }
      }
    }

    def updateLearningStepStatusV2(learningPathId: Long,
                                   learningStepId: Long,
                                   newStatus: StepStatus.Value,
                                   owner: UserInfo): Option[LearningStepV2] = {
      withId(learningPathId).map(_.canEditLearningpath(owner)) match {
        case None              => None
        case Some(Failure(ex)) => throw ex
        case Some(Success(learningPath)) =>
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None => None
            case Some(learningStep) =>
              val stepToUpdate = learningStep.copy(status = newStatus)
              val stepsToChangeSeqNoOn = learningPathRepository
                .learningStepsFor(learningPathId)
                .filter(step => step.seqNo >= stepToUpdate.seqNo && step.id != stepToUpdate.id)

              val stepsWithChangedSeqNo = stepToUpdate.status match {
                case StepStatus.DELETED =>
                  stepsToChangeSeqNoOn.map(step => step.copy(seqNo = step.seqNo - 1))
                case StepStatus.ACTIVE =>
                  stepsToChangeSeqNoOn.map(step => step.copy(seqNo = step.seqNo + 1))
              }

              val (updatedPath, updatedStep) = inTransaction { implicit session =>
                val updatedStep = learningPathRepository.updateLearningStep(stepToUpdate)

                val newLearningSteps = learningPath.learningsteps.filterNot(
                  step =>
                    stepsWithChangedSeqNo
                      .map(_.id)
                      .contains(step.id)) ++ stepsWithChangedSeqNo

                val lp = converterService.insertLearningSteps(learningPath, newLearningSteps, owner)
                val updatedPath = learningPathRepository.update(lp)

                stepsWithChangedSeqNo.foreach(learningPathRepository.updateLearningStep)

                (updatedPath, updatedStep)
              }

              if (updatedPath.isPublished) {
                searchIndexService.indexDocument(updatedPath)
              } else {
                deleteIsBasedOnReference(learningPath)
                searchIndexService.deleteDocument(learningPath)
              }

              converterService.asApiLearningStepV2(updatedStep, updatedPath, Language.DefaultLanguage, true, owner)
          }
      }
    }

    def updateConfig(configKey: ConfigKey.Value, value: UpdateConfigValue, userInfo: UserInfo) = {
      val newConfigValue = ConfigValue(configKey, value.value, new Date(), userInfo.userId)
      configRepository.updateConfigParam(newConfigValue)
    }

    def updateSeqNo(learningPathId: Long,
                    learningStepId: Long,
                    seqNo: Int,
                    owner: UserInfo): Option[LearningStepSeqNo] = {
      optimisticLockRetries(10) {
        withId(learningPathId) match {
          case None => None
          case Some(learningPath) =>
            learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
              case None => None
              case Some(learningStep) => {
                learningPath.validateSeqNo(seqNo)

                val from = learningStep.seqNo
                val to = seqNo
                val toUpdate = learningPath.learningsteps.filter(step => rangeToUpdate(from, to).contains(step.seqNo))

                def addOrSubtract(seqNo: Int): Int = from > to match {
                  case true  => seqNo + 1
                  case false => seqNo - 1
                }

                inTransaction { implicit session =>
                  learningPathRepository.updateLearningStep(learningStep.copy(seqNo = seqNo))
                  toUpdate.foreach(step => {
                    learningPathRepository.updateLearningStep(step.copy(seqNo = addOrSubtract(step.seqNo)))
                  })
                }

                Some(LearningStepSeqNo(seqNo))
              }
            }
        }
      }
    }

    def rangeToUpdate(from: Int, to: Int): Range = {
      from > to match {
        case true  => to until from
        case false => from + 1 to to
      }
    }

    private def withId(learningPathId: Long, includeDeleted: Boolean = false): Option[domain.LearningPath] = {
      if (includeDeleted) {
        learningPathRepository.withIdIncludingDeleted(learningPathId)
      } else {
        learningPathRepository.withId(learningPathId)
      }
    }

    def optimisticLockRetries[T](n: Int)(fn: => T): T = {
      try {
        fn
      } catch {
        case ole: OptimisticLockException =>
          if (n > 1) optimisticLockRetries(n - 1)(fn) else throw ole
        case t: Throwable => throw t
      }
    }
  }

}
