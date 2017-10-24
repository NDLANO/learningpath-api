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
import no.ndla.learningpathapi.model.domain.{LearningPathStatus, Title, LearningPath => _, LearningStep => _, _}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.search.SearchIndexServiceComponent
import no.ndla.learningpathapi.validation.{LearningPathValidator, LearningStepValidator}
import com.netaporter.uri.dsl._

trait UpdateServiceComponent {
  this: LearningPathRepositoryComponent with ConverterServiceComponent with SearchIndexServiceComponent with Clock with LearningStepValidator with LearningPathValidator =>
  val updateService: UpdateService

  class UpdateService {
    def newFromExisting(id: Long, newLearningPath: NewCopyLearningPath, owner: String): Option[LearningPath] = {
      learningPathRepository.withId(id) match {
        case None => None
        case Some(existing) => {
          existing.verifyOwnerOrPublic(Some(owner))

          val title = mergeLanguageFields[Title](existing.title, newLearningPath.title.map(converterService.asTitle))
          val description = mergeLanguageFields(existing.description, newLearningPath.description.map(converterService.asDescription))
          val tags = if(newLearningPath.tags.nonEmpty) newLearningPath.tags.map(converterService.asLearningPathTags) else existing.tags
          val coverPhotoId = newLearningPath.coverPhotoMetaUrl.map(extractImageId).getOrElse(existing.coverPhotoId)
          val duration = if(newLearningPath.duration.nonEmpty) newLearningPath.duration else existing.duration
          val copyright = newLearningPath.copyright.map(converterService.asCopyright).getOrElse(existing.copyright)
          val toInsert = existing.copy(
            id = None,
            revision = None,
            externalId = None,
            isBasedOn = if (existing.isPrivate) None else existing.id,
            title = title,
            description = description,
            status = LearningPathStatus.PRIVATE,
            verificationStatus = LearningPathVerificationStatus.EXTERNAL,
            lastUpdated = clock.now(),
            owner = owner,
            copyright = copyright,
            learningsteps = existing.learningsteps.map(_.copy(id = None, revision = None, externalId = None, learningPathId = None)),
            tags = tags,
            coverPhotoId = coverPhotoId,
            duration = duration)
          learningPathValidator.validate(toInsert, allowUnknownLanguage = true)
          Some(converterService.asApiLearningpath(learningPathRepository.insert(toInsert), Some(owner)))
        }
      }
    }

    def newFromExistingV2(id: Long, newLearningPath: NewCopyLearningPathV2, owner: String): Option[LearningPathV2] = {
      learningPathRepository.withId(id) match {
        case None => None
        case Some(existing) => {
          existing.verifyOwnerOrPublic(Some(owner))
          val oldTitle = Seq(domain.Title(newLearningPath.title, newLearningPath.language))

          val oldDescription = newLearningPath.description match {
            case None => Seq.empty
            case Some(value) => Seq(domain.Description(value, newLearningPath.language))
          }

          val oldTags = newLearningPath.tags match {
            case None => Seq.empty
            case Some(value) => Seq(domain.LearningPathTags(value, newLearningPath.language))
          }

          val title = mergeLanguageFields[Title](existing.title, oldTitle)
          val description = mergeLanguageFields(existing.description, oldDescription)
          val tags = mergeLearningPathTags(existing.tags, oldTags)
          val coverPhotoId = newLearningPath.coverPhotoMetaUrl.map(extractImageId).getOrElse(existing.coverPhotoId)
          val duration = if(newLearningPath.duration.nonEmpty) newLearningPath.duration else existing.duration
          val copyright = newLearningPath.copyright.map(converterService.asCopyright).getOrElse(existing.copyright)
          val toInsert = existing.copy(
            id = None,
            revision = None,
            externalId = None,
            isBasedOn = if (existing.isPrivate) None else existing.id,
            title = title,
            description = description,
            status = LearningPathStatus.PRIVATE,
            verificationStatus = LearningPathVerificationStatus.EXTERNAL,
            lastUpdated = clock.now(),
            owner = owner,
            copyright = copyright,
            learningsteps = existing.learningsteps.map(_.copy(id = None, revision = None, externalId = None, learningPathId = None)),
            tags = tags,
            coverPhotoId = coverPhotoId,
            duration = duration)
          learningPathValidator.validate(toInsert, allowUnknownLanguage = true)
          converterService.asApiLearningpathV2(learningPathRepository.insert(toInsert), newLearningPath.language, Some(owner))
        }
      }
    }

    def addLearningPath(newLearningPath: NewLearningPath, owner: String): LearningPath = {
      val learningPath = domain.LearningPath(None, None, None, None,
        newLearningPath.title.map(converterService.asTitle),
        newLearningPath.description.map(converterService.asDescription),
        newLearningPath.coverPhotoMetaUrl.flatMap(extractImageId),
        newLearningPath.duration, domain.LearningPathStatus.PRIVATE,
        LearningPathVerificationStatus.EXTERNAL,
        clock.now(), newLearningPath.tags.map(converterService.asLearningPathTags), owner,
        converterService.asCopyright(newLearningPath.copyright), List())
      learningPathValidator.validate(learningPath)

      converterService.asApiLearningpath(learningPathRepository.insert(learningPath), Option(owner))
    }

    def addLearningPathV2(newLearningPath: NewLearningPathV2, owner: String): Option[LearningPathV2] = {
      val domainTags =
        if (newLearningPath.tags.isEmpty) Seq.empty
        else Seq(domain.LearningPathTags(newLearningPath.tags, newLearningPath.language))

      val learningPath = domain.LearningPath(None, None, None, None,
        Seq(domain.Title(newLearningPath.title, newLearningPath.language)),
        Seq(domain.Description(newLearningPath.description, newLearningPath.language)),
        newLearningPath.coverPhotoMetaUrl.flatMap(extractImageId),
        newLearningPath.duration, domain.LearningPathStatus.PRIVATE,
        LearningPathVerificationStatus.EXTERNAL,
        clock.now(),
        domainTags,
        owner,
        converterService.asCopyright(newLearningPath.copyright), List())
      learningPathValidator.validate(learningPath)

      converterService.asApiLearningpathV2(learningPathRepository.insert(learningPath), newLearningPath.language, Option(owner))
    }

    private def extractImageId(url: String): Option[String] = {
      learningPathValidator.validateCoverPhoto(url) match {
        case Some(err) => throw new ValidationException(errors=Seq(err))
        case _ =>
      }

      val pattern = """.*/images/(\d+)""".r
      pattern.findFirstMatchIn(url.path).map(_.group(1))
    }

    def mergeLanguageFields[A <: LanguageField[String]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.value.isEmpty)
    }

    def mergeLearningPathTags(existing: Seq[domain.LearningPathTags], updated: Seq[domain.LearningPathTags]): Seq[domain.LearningPathTags] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    def updateLearningPath(id: Long, learningPathToUpdate: UpdatedLearningPath, owner: String): Option[LearningPath] = {
      withIdAndAccessGranted(id, owner) match {
        case None => None
        case Some(existing) => {
          val toUpdate = existing.copy(
            revision = Some(learningPathToUpdate.revision),
            title = mergeLanguageFields(existing.title, learningPathToUpdate.title.map(converterService.asTitle)),
            description = mergeLanguageFields(existing.description, learningPathToUpdate.description.map(converterService.asDescription)),
            coverPhotoId = learningPathToUpdate.coverPhotoMetaUrl.map(extractImageId).getOrElse(existing.coverPhotoId),
            duration = if(learningPathToUpdate.duration.isDefined) learningPathToUpdate.duration else existing.duration,
            tags = mergeLearningPathTags(existing.tags, learningPathToUpdate.tags.map(converterService.asLearningPathTags)),
            copyright = converterService.asCopyright(learningPathToUpdate.copyright),
            lastUpdated = clock.now())
          learningPathValidator.validate(toUpdate)


          val updatedLearningPath = learningPathRepository.update(toUpdate)
          if (updatedLearningPath.isPublished) {
            searchIndexService.indexDocument(updatedLearningPath)
          }

          Some(converterService.asApiLearningpath(updatedLearningPath, Option(owner)))
        }
      }
    }

    def updateLearningPathV2(id: Long, learningPathToUpdate: UpdatedLearningPathV2, owner: String): Option[LearningPathV2] = {
      // Should not be able to submit with an illegal language
      learningPathValidator.validate(learningPathToUpdate)

      val titles = learningPathToUpdate.title match {
        case None => Seq.empty
        case Some(value) => Seq(domain.Title(value, learningPathToUpdate.language))
      }

      val descriptions = learningPathToUpdate.description match {
        case None => Seq.empty
        case Some(value) => Seq(domain.Description(value, learningPathToUpdate.language))
      }

      val tags = learningPathToUpdate.tags match {
        case None => Seq.empty
        case Some(value) => Seq(domain.LearningPathTags(value, learningPathToUpdate.language))
      }

      withIdAndAccessGranted(id, owner) match {
        case None => None
        case Some(existing) => {
          val toUpdate = existing.copy(
            revision = Some(learningPathToUpdate.revision),
            title = mergeLanguageFields(existing.title, titles),
            description = mergeLanguageFields(existing.description, descriptions),
            coverPhotoId = learningPathToUpdate.coverPhotoMetaUrl.map(extractImageId).getOrElse(existing.coverPhotoId),
            duration = if(learningPathToUpdate.duration.isDefined) learningPathToUpdate.duration else existing.duration,
            tags = mergeLearningPathTags(existing.tags, tags),
            copyright = if (learningPathToUpdate.copyright.isDefined) converterService.asCopyright(learningPathToUpdate.copyright.get) else existing.copyright,
            lastUpdated = clock.now())
          // Imported learningpaths may contain fields with language=unknown.
          // We should still be able to update it, but not add new fields with language=unknown.
          learningPathValidator.validate(toUpdate, allowUnknownLanguage=true)

          val updatedLearningPath = learningPathRepository.update(toUpdate)
          if (updatedLearningPath.isPublished) {
            searchIndexService.indexDocument(updatedLearningPath)
          }

          converterService.asApiLearningpathV2(updatedLearningPath, learningPathToUpdate.language, Option(owner))
        }
      }
    }

    def updateLearningPathStatus(learningPathId: Long, status: LearningPathStatus.Value, owner: String): Option[LearningPath] = {
      withIdAndAccessGranted(learningPathId, owner, includeDeleted = true) match {
        case None => None
        case Some(existing) => {
          if (status == domain.LearningPathStatus.PUBLISHED) {
            existing.validateForPublishing()
          }

          val updatedLearningPath = learningPathRepository.update(
            existing.copy(
              status = status,
              lastUpdated = clock.now()))

          if (updatedLearningPath.isPublished) {
            searchIndexService.indexDocument(updatedLearningPath)
          } else if (existing.isPublished) {
            searchIndexService.deleteDocument(updatedLearningPath)
            deleteIsBasedOnReference(updatedLearningPath)
          }

          Some(converterService.asApiLearningpath(updatedLearningPath, Option(owner)))
        }
      }
    }

    private def deleteIsBasedOnReference(updatedLearningPath: domain.LearningPath) = {
      learningPathRepository.learningPathsWithIsBasedOn(updatedLearningPath.id.get).foreach(lp => {
        learningPathRepository.update(
          lp.copy(
            lastUpdated = clock.now(),
            isBasedOn = None
          )
        )
      })
    }

    /*
    //TODO: Remove comment
    def addLearningStep(learningPathId: Long, newLearningStep: NewLearningStepV2, owner: String): Option[LearningStepV2] = {
      optimisticLockRetries(10) {
        withIdAndAccessGranted(learningPathId, owner) match {
          case None => None
          case Some(learningPath) => {
            val newSeqNo = learningPath.learningsteps.isEmpty match {
              case true => 0
              case false => learningPath.learningsteps.map(_.seqNo).max + 1
            }

            /*val newStep = domain.LearningStep(None, None, None, learningPath.id, newSeqNo,
              newLearningStep.title.map(converterService.asTitle),
              newLearningStep.description.map(converterService.asDescription),
              newLearningStep.embedUrl.map(converterService.asEmbedUrlV2),
              StepType.valueOfOrError(newLearningStep.`type`),
              newLearningStep.license,
              newLearningStep.showTitle)
            learningStepValidator.validate(newStep)
            */
            val newStep = domain.LearningStep(None, None, None, learningPath.id, newSeqNo,
              newLearningStep.title
            )

            val (insertedStep, updatedPath) = inTransaction { implicit session =>
              val insertedStep = learningPathRepository.insertLearningStep(newStep)
              val updatedPath = learningPathRepository.update(learningPath.copy(
                learningsteps = learningPath.learningsteps :+ insertedStep,
                lastUpdated = clock.now()))

              (insertedStep, updatedPath)
            }
            if (updatedPath.isPublished) {
              searchIndexService.indexDocument(updatedPath)
            }
          Some(converterService.asApiLearningStep(insertedStep, updatedPath, Option(owner)))
          }
        }
      }
    }
    */

    def addLearningStepV2(learningPathId: Long, newLearningStep: NewLearningStepV2, owner: String): Option[LearningStepV2] = {
      optimisticLockRetries(10) {
        withIdAndAccessGranted(learningPathId, owner) match {
          case None => None
          case Some(learningPath) => {
            val description = newLearningStep.description match {
              case None => Seq.empty
              case Some(value) => Seq(domain.Description(value, newLearningStep.language))
            }

            val embedUrl = newLearningStep.embedUrl match {
              case None => Seq.empty
              case Some(value) => Seq(domain.EmbedUrl(value.url, newLearningStep.language, EmbedType.valueOfOrError(value.embedType)))
            }

            val newSeqNo = learningPath.learningsteps.isEmpty match {
              case true => 0
              case false => learningPath.learningsteps.map(_.seqNo).max + 1
            }

            val newStep = domain.LearningStep(None, None, None, learningPath.id, newSeqNo,
              Seq(domain.Title(newLearningStep.title, newLearningStep.language)),
              description,
              embedUrl,
              StepType.valueOfOrError(newLearningStep.`type`),
              newLearningStep.license,
              newLearningStep.showTitle)
            learningStepValidator.validate(newStep)

            val (insertedStep, updatedPath) = inTransaction { implicit session =>
              val insertedStep = learningPathRepository.insertLearningStep(newStep)
              val updatedPath = learningPathRepository.update(learningPath.copy(
                learningsteps = learningPath.learningsteps :+ insertedStep,
                lastUpdated = clock.now()))

              (insertedStep, updatedPath)
            }
            if (updatedPath.isPublished) {
              searchIndexService.indexDocument(updatedPath)
            }
            converterService.asApiLearningStepV2(insertedStep, updatedPath, newLearningStep.language, Option(owner))
          }
        }
      }
    }

    /*
    // TODO: Remove this
    def updateLearningStep(learningPathId: Long, learningStepId: Long, learningStepToUpdate: UpdatedLearningStep, owner: String): Option[LearningStep] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => None
        case Some(learningPath) => {
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None => None
            case Some(existing) => {
              val titles = mergeLanguageFields(existing.title, learningStepToUpdate.title.map(converterService.asTitle))
              val descriptions = mergeLanguageFields(existing.description, learningStepToUpdate.description.map(converterService.asDescription))
              val embedUrls = mergeLanguageFields(existing.embedUrl, learningStepToUpdate.embedUrl.map(converterService.asEmbedUrl))

              val toUpdate = existing.copy(
                revision = Some(learningStepToUpdate.revision),
                title = titles,
                description = descriptions,
                embedUrl = embedUrls,
                showTitle = learningStepToUpdate.showTitle.getOrElse(existing.showTitle),
                `type` = learningStepToUpdate.`type`.map(domain.StepType.valueOfOrError).getOrElse(existing.`type`),
                license = learningStepToUpdate.license
              )
              learningStepValidator.validate(toUpdate)
              val (updatedStep, updatedPath) = inTransaction { implicit session =>
                val updatedStep = learningPathRepository.updateLearningStep(toUpdate)
                val updatedPath = learningPathRepository.update(learningPath.copy(
                  learningsteps = learningPath.learningsteps.filterNot(_.id == updatedStep.id) :+ updatedStep,
                  lastUpdated = clock.now()))

                (updatedStep, updatedPath)
              }

              if (updatedPath.isPublished) {
                searchIndexService.indexDocument(updatedPath)
              }

              Some(converterService.asApiLearningStep(updatedStep, updatedPath, Option(owner)))
            }
          }
        }
      }
    }
    */

    def updateLearningStepV2(learningPathId: Long, learningStepId: Long, learningStepToUpdate: UpdatedLearningStepV2, owner: String): Option[LearningStepV2] = {
      withIdAndAccessGranted(learningPathId, owner) match {
        case None => None
        case Some(learningPath) => {
          learningPathRepository.learningStepWithId(learningPathId, learningStepId) match {
            case None => None
            case Some(existing) => {
              val titles = learningStepToUpdate.title match {
                case None => Seq.empty
                case Some(value) => mergeLanguageFields(existing.title, Seq(domain.Title(value, learningStepToUpdate.language)))
              }

              val descriptions = learningStepToUpdate.description match {
                case None => Seq.empty
                case Some(value) => mergeLanguageFields(existing.description, Seq(domain.Description(value, learningStepToUpdate.language)))
              }

              val embedUrls = learningStepToUpdate.embedUrl match {
                case None => Seq.empty
                case Some(value) => mergeLanguageFields(existing.embedUrl, Seq(domain.EmbedUrl(value.url, learningStepToUpdate.language, EmbedType.valueOfOrError(value.embedType))))
              }

              val toUpdate = existing.copy(
                revision = Some(learningStepToUpdate.revision),
                title = titles,
                description = descriptions,
                embedUrl = embedUrls,
                showTitle = learningStepToUpdate.showTitle.getOrElse(existing.showTitle),
                `type` = learningStepToUpdate.`type`.map(domain.StepType.valueOfOrError).getOrElse(existing.`type`),
                license = learningStepToUpdate.license
              )
              learningStepValidator.validate(toUpdate)
              val (updatedStep, updatedPath) = inTransaction { implicit session =>
                val updatedStep = learningPathRepository.updateLearningStep(toUpdate)
                val updatedPath = learningPathRepository.update(learningPath.copy(
                  learningsteps = learningPath.learningsteps.filterNot(_.id == updatedStep.id) :+ updatedStep,
                  lastUpdated = clock.now()))

                (updatedStep, updatedPath)
              }

              if (updatedPath.isPublished) {
                searchIndexService.indexDocument(updatedPath)
              }

              converterService.asApiLearningStepV2(updatedStep, updatedPath, learningStepToUpdate.language, Option(owner))
            }
          }
        }
      }
    }

    def updateLearningStepStatusV2(learningPathId: Long, learningStepId: Long, newStatus: StepStatus.Value, owner:String): Option[LearningStepV2] = {
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
                searchIndexService.indexDocument(updatedPath)
              }

              converterService.asApiLearningStepV2(updatedStep, updatedPath, Language.DefaultLanguage, Option(owner))
            }
          }
        }
      }
    }

    /*
    // TODO: Remove
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
                searchIndexService.indexDocument(updatedPath)
              }

              Some(converterService.asApiLearningStep(updatedStep, updatedPath, Option(owner)))
            }
          }
        }
      }
    }
    */

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

    private def withIdAndAccessGranted(learningPathId: Long, owner: String, includeDeleted: Boolean = false): Option[domain.LearningPath] = {
      val learningPath = includeDeleted match {
        case false => learningPathRepository.withId(learningPathId)
        case true => learningPathRepository.withIdIncludingDeleted(learningPathId)
      }

      accessGranted(learningPath, owner)
    }

    private def accessGranted(learningPath: Option[domain.LearningPath], owner: String): Option[domain.LearningPath] = {
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
