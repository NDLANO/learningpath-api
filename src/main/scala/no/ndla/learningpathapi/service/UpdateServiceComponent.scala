package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.{LearningPathStatus, LearningPath => _, LearningStep => _, _}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexServiceComponent


trait UpdateServiceComponent {
  this: LearningPathRepositoryComponent with ConverterServiceComponent with SearchIndexServiceComponent with Clock =>
  val updateService: UpdateService

  class UpdateService {
    def newFromExisting(id: Long, newLearningPath: NewLearningPath, owner: String): Option[LearningPath] = {
      learningPathRepository.withId(id) match {
        case None => None
        case Some(existing) => {
          existing.verifyOwnerOrPublic(Some(owner))

          val title = if(newLearningPath.title.nonEmpty) newLearningPath.title.map(converterService.asTitle) else existing.title
          val description = if(newLearningPath.description.nonEmpty) newLearningPath.description.map(converterService.asDescription) else existing.description
          val tags = if(newLearningPath.tags.nonEmpty) newLearningPath.tags.map(converterService.asLearningPathTag) else existing.tags
          val coverPhotoMetaUrl = if(newLearningPath.coverPhotoMetaUrl.nonEmpty) newLearningPath.coverPhotoMetaUrl else existing.coverPhotoMetaUrl
          val duration = if(newLearningPath.duration.nonEmpty) newLearningPath.duration else existing.duration

          val toInsert = existing.copy(
            id = None,
            revision = None,
            externalId = None,
            isBasedOn = existing.id,
            title = title,
            description = description,
            status = LearningPathStatus.PRIVATE,
            verificationStatus = LearningPathVerificationStatus.EXTERNAL,
            lastUpdated = clock.now(),
            owner = owner,
            learningsteps = existing.learningsteps.map(_.copy(id = None, revision = None, externalId = None, learningPathId = None)),
            tags = tags,
            coverPhotoMetaUrl = coverPhotoMetaUrl,
            duration = duration)

          Some(converterService.asApiLearningpath(learningPathRepository.insert(toInsert), Some(owner)))
        }
      }
    }


    def addLearningPath(newLearningPath: NewLearningPath, owner: String): LearningPath = {
      val learningPath = domain.LearningPath(None, None, None, None,
        newLearningPath.title.map(converterService.asTitle),
        newLearningPath.description.map(converterService.asDescription),
        newLearningPath.coverPhotoMetaUrl,
        newLearningPath.duration, domain.LearningPathStatus.PRIVATE,
        LearningPathVerificationStatus.EXTERNAL,
        clock.now(), newLearningPath.tags.map(converterService.asLearningPathTag), owner, List())

      converterService.asApiLearningpath(learningPathRepository.insert(learningPath), Option(owner))
    }

    def updateLearningPath(id: Long, learningPathToUpdate: UpdatedLearningPath, owner: String): Option[LearningPath] = {
      withIdAndAccessGranted(id, owner) match {
        case None => None
        case Some(existing) => {
          val toUpdate = existing.copy(
            revision = Some(learningPathToUpdate.revision),
            title = learningPathToUpdate.title.map(converterService.asTitle),
            description = learningPathToUpdate.description.map(converterService.asDescription),
            coverPhotoMetaUrl = learningPathToUpdate.coverPhotoMetaUrl,
            duration = learningPathToUpdate.duration,
            tags = learningPathToUpdate.tags.map(converterService.asLearningPathTag),
            lastUpdated = clock.now())

          val updatedLearningPath = learningPathRepository.update(toUpdate)
          if (updatedLearningPath.isPublished) {
            searchIndexService.indexLearningPath(updatedLearningPath)
          }

          Some(converterService.asApiLearningpath(updatedLearningPath, Option(owner)))
        }
      }
    }

    def updateLearningPathStatus(learningPathId: Long, status: LearningPathStatus, owner: String): Option[LearningPath] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => None
        case Some(existing) => {
          val newStatus = domain.LearningPathStatus.valueOfOrDefault(status.status)
          if (newStatus == domain.LearningPathStatus.PUBLISHED) {
            existing.validateForPublishing()
          }

          val updatedLearningPath = learningPathRepository.update(
            existing.copy(
              status = newStatus,
              lastUpdated = clock.now()))

          updatedLearningPath.isPublished match {
            case true => searchIndexService.indexLearningPath(updatedLearningPath)
            case false => searchIndexService.deleteLearningPath(updatedLearningPath)
          }


          Some(converterService.asApiLearningpath(updatedLearningPath, Option(owner)))
        }
      }
    }

    def deleteLearningPath(learningPathId: Long, owner: String): Boolean = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => false
        case Some(existing) => {
          learningPathRepository.delete(learningPathId)
          searchIndexService.deleteLearningPath(existing)
          true
        }
      }
    }

    def addLearningStep(learningPathId: Long, newLearningStep: NewLearningStep, owner: String): Option[LearningStep] = {
      optimisticLockRetries(10) {
        withIdAndAccessGranted(learningPathId, owner) match {
          case None => None
          case Some(learningPath) => {
            val newSeqNo = learningPath.learningsteps.isEmpty match {
              case true => 0
              case false => learningPath.learningsteps.map(_.seqNo).max + 1
            }

            val newStep = domain.LearningStep(None, None, None, learningPath.id, newSeqNo,
              newLearningStep.title.map(converterService.asTitle),
              newLearningStep.description.map(converterService.asDescription),
              newLearningStep.embedContent.map(converterService.asEmbedUrl),
              StepType.valueOfOrDefault(newLearningStep.`type`),
              newLearningStep.license,
              newLearningStep.showTitle)

            val (insertedStep, updatedPath) = inTransaction { implicit session =>
              val insertedStep = learningPathRepository.insertLearningStep(newStep)
              val updatedPath = learningPathRepository.update(learningPath.copy(
                learningsteps = learningPath.learningsteps :+ insertedStep,
                lastUpdated = clock.now()))

              (insertedStep, updatedPath)
            }
            if (updatedPath.isPublished) {
              searchIndexService.indexLearningPath(updatedPath)
            }
          Some(converterService.asApiLearningStep(insertedStep, updatedPath, Option(owner)))
          }
        }
      }
    }


    def updateLearningStep(learningPathId: Long, learningStepId: Long, learningStepToUpdate: UpdatedLearningStep, owner: String): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => None
        case Some(learningPath) => {
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None => None
            case Some(existing) => {
              val toUpdate = existing.copy(
                revision = Some(learningStepToUpdate.revision),
                title = learningStepToUpdate.title.map(converterService.asTitle),
                description = learningStepToUpdate.description.map(converterService.asDescription),
                embedUrl = learningStepToUpdate.embedContent.map(converterService.asEmbedUrl),
                showTitle = learningStepToUpdate.showTitle,
                `type` = domain.StepType.valueOfOrDefault(learningStepToUpdate.`type`),
                license = learningStepToUpdate.license)

              val (updatedStep, updatedPath) = inTransaction { implicit session =>
                val updatedStep = learningPathRepository.updateLearningStep(toUpdate)
                val updatedPath = learningPathRepository.update(learningPath.copy(
                  learningsteps = learningPath.learningsteps.filterNot(_.id == updatedStep.id) :+ updatedStep,
                  lastUpdated = clock.now()))

                (updatedStep, updatedPath)
              }

              if (updatedPath.isPublished) {
                searchIndexService.indexLearningPath(updatedPath)
              }

              Some(converterService.asApiLearningStep(updatedStep, updatedPath, Option(owner)))
            }
          }
        }
      }
    }

    def updateLearningStepStatus(learningPathId: Long, learningStepId: Long, newStatus: StepStatus.Value, owner:String): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => None
        case Some(learningPath) => {
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None => None
            case Some(learningStep) => {
              val stepToUpdate = learningStep.copy(status = newStatus)
              val stepsToChangeSeqNoOn = learningPathRepository.learningStepsFor(learningPathId).filter(step => step.seqNo >= stepToUpdate.seqNo && step.id != stepToUpdate.id)

              val stepsWithChangedSeqNo = stepToUpdate.status match {
                case StepStatus.DELETED => stepsToChangeSeqNoOn.map(step => step.copy(seqNo = step.seqNo - 1))
                case StepStatus.ACTIVE => stepsToChangeSeqNoOn.map(step => step.copy(seqNo = step.seqNo + 1))
              }

              val (updatedPath, updatedStep) = inTransaction{ implicit session =>
                val updatedStep = learningPathRepository.updateLearningStep(stepToUpdate)
                stepsWithChangedSeqNo.foreach(learningPathRepository.updateLearningStep)

                val newLearningSteps = learningPath.learningsteps.filterNot(step => stepsWithChangedSeqNo.map(_.id).contains(step.id)) ++ stepsWithChangedSeqNo

                val updatedPath = learningPathRepository.update(learningPath.copy(
                  learningsteps = if(StepStatus.ACTIVE == updatedStep.status) newLearningSteps :+ updatedStep else newLearningSteps,
                  lastUpdated = clock.now()))

                (updatedPath, updatedStep)
              }

              if (updatedPath.isPublished) {
                searchIndexService.indexLearningPath(updatedPath)
              }

              Some(converterService.asApiLearningStep(updatedStep, updatedPath, Option(owner)))
            }
          }
        }
      }
    }


    def updateSeqNo(learningPathId: Long, learningStepId: Long, seqNo: Int, owner: String): Option[LearningStepSeqNo] = {
      optimisticLockRetries(10) {
        withIdAndAccessGranted(learningPathId, owner) match {
          case None => None
          case Some(learningPath) => {
            learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
              case None => None
              case Some(learningStep) => {
                learningPath.validateSeqNo(seqNo)

                val from = learningStep.seqNo
                val to = seqNo
                val toUpdate = learningPath.learningsteps.filter(step => rangeToUpdate(from, to).contains(step.seqNo))

                def addOrSubtract(seqNo: Int): Int = from > to match {
                  case true => seqNo + 1
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
    }

    def rangeToUpdate(from: Int, to: Int): Range = {
      from > to match {
        case true => to until from
        case false => from + 1 to to
      }
    }

    private def withIdAndAccessGranted(learningPathId: Long, owner: String): Option[domain.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.foreach(_.verifyOwner(owner))
      learningPath
    }

    def optimisticLockRetries[T](n: Int)(fn: => T): T = {
      try {
        fn
      } catch {
        case ole: OptimisticLockException => if (n > 1) optimisticLockRetries(n - 1)(fn) else throw ole
        case t: Throwable => throw t
      }
    }
  }

}
