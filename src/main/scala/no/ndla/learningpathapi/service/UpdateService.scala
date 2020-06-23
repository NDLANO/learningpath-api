/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.model.api.{config, _}
import no.ndla.learningpathapi.model.api.config.UpdateConfigValue
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
import no.ndla.learningpathapi.model.domain.{LearningPathStatus, UserInfo, LearningPath => _, LearningStep => _, _}
import no.ndla.learningpathapi.repository.{ConfigRepository, LearningPathRepositoryComponent}
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.learningpathapi.validation.{LearningPathValidator, LearningStepValidator}

import scala.util.{Failure, Success, Try}

trait UpdateService {
  this: LearningPathRepositoryComponent
    with ReadService
    with ConfigRepository
    with ConverterService
    with SearchIndexService
    with Clock
    with LearningStepValidator
    with LearningPathValidator =>
  val updateService: UpdateService

  class UpdateService {

    def insertDump(dump: domain.LearningPath) = {
      learningPathRepository.insert(dump)
    }

    private[service] def writeDuringWriteRestrictionOrAccessDenied[T](owner: UserInfo)(w: => Try[T]): Try[T] =
      writeOrAccessDenied(readService.canWriteNow(owner),
                          "You do not have write access while write restriction is active.")(w)

    private[service] def writeOrAccessDenied[T](
        willExecute: Boolean,
        reason: String = "You do not have permission to perform this action.")(w: => Try[T]): Try[T] =
      if (willExecute) w
      else Failure(AccessDeniedException(reason))

    def newFromExistingV2(id: Long, newLearningPath: NewCopyLearningPathV2, owner: UserInfo): Try[LearningPathV2] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        learningPathRepository.withId(id).map(_.isOwnerOrPublic(owner)) match {
          case None              => Failure(NotFoundException("Could not find learningpath to copy."))
          case Some(Failure(ex)) => Failure(ex)
          case Some(Success(existing)) =>
            val toInsert = converterService.newFromExistingLearningPath(existing, newLearningPath, owner)
            learningPathValidator.validate(toInsert, allowUnknownLanguage = true)
            converterService.asApiLearningpathV2(learningPathRepository.insert(toInsert),
                                                 newLearningPath.language,
                                                 fallback = true,
                                                 owner)
        }
      }

    def addLearningPathV2(newLearningPath: NewLearningPathV2, owner: UserInfo): Try[LearningPathV2] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        val learningPath = converterService.newLearningPath(newLearningPath, owner)
        learningPathValidator.validate(learningPath)

        converterService.asApiLearningpathV2(learningPathRepository.insert(learningPath),
                                             newLearningPath.language,
                                             fallback = true,
                                             owner)
      }

    def updateLearningPathV2(id: Long,
                             learningPathToUpdate: UpdatedLearningPathV2,
                             owner: UserInfo): Try[LearningPathV2] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      learningPathValidator.validate(learningPathToUpdate)

      withId(id).flatMap(_.canEditLearningpath(owner)) match {
        case Failure(ex) => Failure(ex)
        case Success(existing) =>
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

          converterService.asApiLearningpathV2(updatedLearningPath,
                                               learningPathToUpdate.language,
                                               fallback = true,
                                               owner)
      }
    }

    def updateLearningPathStatusV2(learningPathId: Long,
                                   status: LearningPathStatus.Value,
                                   owner: UserInfo,
                                   language: String,
                                   message: Option[String] = None): Try[LearningPathV2] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        withId(learningPathId, includeDeleted = true).flatMap(_.canSetStatus(status, owner)) match {
          case Failure(ex) => Failure(ex)
          case Success(existing) =>
            val validatedLearningPath =
              if (status == domain.LearningPathStatus.PUBLISHED) existing.validateForPublishing() else Success(existing)

            validatedLearningPath.flatMap(valid => {
              val newMessage = message match {
                case Some(msg) if owner.isAdmin => Some(domain.Message(msg, owner.userId, clock.now()))
                case _                          => valid.message
              }

              val updatedLearningPath = learningPathRepository.update(
                valid.copy(message = newMessage, status = status, lastUpdated = clock.now()))

              if (updatedLearningPath.isPublished) {
                searchIndexService.indexDocument(updatedLearningPath)
              } else {
                deleteIsBasedOnReference(updatedLearningPath)
                searchIndexService.deleteDocument(updatedLearningPath)
              }

              converterService.asApiLearningpathV2(updatedLearningPath, language, fallback = true, owner)
            })
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
                          owner: UserInfo): Try[LearningStepV2] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      optimisticLockRetries(10) {
        withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
          case Failure(ex) => Failure(ex)
          case Success(learningPath) =>
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
            converterService.asApiLearningStepV2(insertedStep,
                                                 updatedPath,
                                                 newLearningStep.language,
                                                 fallback = true,
                                                 owner)
        }
      }
    }

    def updateLearningStepV2(learningPathId: Long,
                             learningStepId: Long,
                             learningStepToUpdate: UpdatedLearningStepV2,
                             owner: UserInfo): Try[LearningStepV2] = writeDuringWriteRestrictionOrAccessDenied(owner) {
      withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
        case Failure(ex) => Failure(ex)
        case Success(learningPath) =>
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None =>
              Failure(NotFoundException(
                s"Could not find learningstep with id '$learningStepId' to update with learningpath id '$learningPathId'."))
            case Some(existing) =>
              val toUpdate = converterService.mergeLearningSteps(existing, learningStepToUpdate)
              learningStepValidator.validate(toUpdate, allowUnknownLanguage = true)

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

              converterService.asApiLearningStepV2(updatedStep,
                                                   updatedPath,
                                                   learningStepToUpdate.language,
                                                   fallback = true,
                                                   owner)
          }
      }
    }

    def updateLearningStepStatusV2(learningPathId: Long,
                                   learningStepId: Long,
                                   newStatus: StepStatus.Value,
                                   owner: UserInfo): Try[LearningStepV2] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
          case Failure(ex) => Failure(ex)
          case Success(learningPath) =>
            learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
              case None =>
                Failure(
                  NotFoundException(
                    s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"))
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

                  val newLearningSteps = learningPath.learningsteps
                    .getOrElse(Seq.empty)
                    .filterNot(
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

                converterService.asApiLearningStepV2(updatedStep,
                                                     updatedPath,
                                                     Language.DefaultLanguage,
                                                     fallback = true,
                                                     owner)
            }
        }
      }

    def updateConfig(configKey: ConfigKey.Value,
                     value: UpdateConfigValue,
                     userInfo: UserInfo): Try[config.ConfigMeta] = {

      writeOrAccessDenied(userInfo.isAdmin, "Only administrators can edit configuration.") {
        ConfigMeta(configKey, value.value, new Date(), userInfo.userId).validate.flatMap(newConfigValue => {
          configRepository.updateConfigParam(newConfigValue).map(converterService.asApiConfig)
        })
      }
    }

    def updateSeqNo(learningPathId: Long, learningStepId: Long, seqNo: Int, owner: UserInfo): Try[LearningStepSeqNo] =
      writeDuringWriteRestrictionOrAccessDenied(owner) {
        optimisticLockRetries(10) {
          withId(learningPathId).flatMap(_.canEditLearningpath(owner)) match {
            case Failure(ex) => Failure(ex)
            case Success(learningPath) =>
              learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
                case None =>
                  None
                  Failure(
                    NotFoundException(
                      s"LearningStep with id $learningStepId in learningPath $learningPathId not found"))
                case Some(learningStep) =>
                  learningPath.validateSeqNo(seqNo)

                  val from = learningStep.seqNo
                  val to = seqNo
                  val toUpdate = learningPath.learningsteps
                    .getOrElse(Seq.empty)
                    .filter(step => rangeToUpdate(from, to).contains(step.seqNo))

                  def addOrSubtract(seqNo: Int): Int = if (from > to) seqNo + 1 else seqNo - 1

                  inTransaction { implicit session =>
                    learningPathRepository.updateLearningStep(learningStep.copy(seqNo = seqNo))
                    toUpdate.foreach(step => {
                      learningPathRepository.updateLearningStep(step.copy(seqNo = addOrSubtract(step.seqNo)))
                    })
                  }

                  Success(LearningStepSeqNo(seqNo))
              }
          }
        }
      }

    def rangeToUpdate(from: Int, to: Int): Range = if (from > to) to until from else from + 1 to to

    private def withId(learningPathId: Long, includeDeleted: Boolean = false): Try[domain.LearningPath] = {
      val lpOpt = if (includeDeleted) {
        learningPathRepository.withIdIncludingDeleted(learningPathId)
      } else {
        learningPathRepository.withId(learningPathId)
      }

      lpOpt match {
        case Some(learningPath) => Success(learningPath)
        case None               => Failure(NotFoundException(s"Could not find learningpath with id '$learningPathId'."))
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
