/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import java.util.Date
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.validation.DurationValidator
import org.json4s.{DefaultFormats, FieldSerializer}
import org.json4s.FieldSerializer._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class LearningPath(id: Option[Long],
                        revision: Option[Int],
                        externalId: Option[String],
                        isBasedOn: Option[Long],
                        title: Seq[Title],
                        description: Seq[Description],
                        coverPhotoId: Option[String],
                        duration: Option[Int],
                        status: LearningPathStatus.Value,
                        verificationStatus: LearningPathVerificationStatus.Value,
                        lastUpdated: Date,
                        tags: Seq[LearningPathTags],
                        owner: String,
                        copyright: Copyright,
                        learningsteps: Option[Seq[LearningStep]] = None,
                        message: Option[Message] = None) {

  def isPrivate: Boolean = {
    status == LearningPathStatus.PRIVATE
  }

  def isPublished: Boolean = {
    status == LearningPathStatus.PUBLISHED
  }

  def canSetStatus(status: LearningPathStatus.Value, user: UserInfo): Try[LearningPath] = {
    if (status == LearningPathStatus.PUBLISHED && !user.canPublish) {
      Failure(AccessDeniedException("You need to be a publisher to publish learningpaths."))
    } else {
      canEditLearningpath(user)
    }
  }

  def canEditLearningpath(user: UserInfo): Try[LearningPath] = {
    if ((user.userId == owner) ||
        user.isAdmin ||
        (user.isWriter && verificationStatus == LearningPathVerificationStatus.CREATED_BY_NDLA)) {
      Success(this)
    } else {
      Failure(AccessDeniedException("You do not have access to the requested resource."))
    }
  }

  def isOwnerOrPublic(user: UserInfo): Try[LearningPath] = {
    if (isPrivate) {
      canEditLearningpath(user)
    } else {
      Success(this)
    }
  }

  def canEdit(userInfo: UserInfo): Boolean = canEditLearningpath(userInfo).isSuccess

  def lsLength = learningsteps.map(_.length).getOrElse(0)

  def validateSeqNo(seqNo: Int): Unit = {
    if (seqNo < 0 || seqNo > lsLength - 1) {
      throw new ValidationException(
        errors = List(ValidationMessage("seqNo", s"seqNo must be between 0 and ${lsLength - 1}")))
    }
  }

  def validateForPublishing(): Try[LearningPath] = {
    val validationResult = new DurationValidator().validateRequired(duration).toList
    if (validationResult.isEmpty)
      Success(this)
    else
      Failure(new ValidationException(errors = validationResult))
  }
}

object LearningPathRole extends Enumeration {
  val ADMIN: LearningPathRole.Value = Value("ADMIN")
  val WRITE: LearningPathRole.Value = Value("WRITE")
  val PUBLISH: LearningPathRole.Value = Value("PUBLISH")

  def valueOf(s: String): Option[LearningPathRole.Value] = {
    val lpRole = s.split("learningpath:")
    LearningPathRole.values.find(_.toString == lpRole.lastOption.getOrElse("").toUpperCase)
  }
}

object LearningPathStatus extends Enumeration {
  val PUBLISHED, PRIVATE, DELETED, UNLISTED, SUBMITTED = Value

  def valueOf(s: String): Option[LearningPathStatus.Value] = {
    LearningPathStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrError(status: String): LearningPathStatus.Value = {
    valueOf(status) match {
      case Some(status) => status
      case None =>
        throw new ValidationException(
          errors = List(ValidationMessage("status", s"'$status' is not a valid publishingstatus.")))
    }
  }

  def valueOfOrDefault(s: String): LearningPathStatus.Value = {
    valueOf(s).getOrElse(LearningPathStatus.PRIVATE)
  }
}

object LearningPathVerificationStatus extends Enumeration {
  val EXTERNAL, CREATED_BY_NDLA, VERIFIED_BY_NDLA = Value

  def valueOf(s: String): Option[LearningPathVerificationStatus.Value] = {
    LearningPathVerificationStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrDefault(s: String): LearningPathVerificationStatus.Value = {
    valueOf(s).getOrElse(LearningPathVerificationStatus.EXTERNAL)
  }
}

object LearningPath extends SQLSyntaxSupport[LearningPath] {

  val jsonSerializer = List(
    new EnumNameSerializer(LearningPathStatus),
    new EnumNameSerializer(LearningPathVerificationStatus)
  )

  val repositorySerializer = jsonSerializer :+ FieldSerializer[LearningPath](
    ignore("id") orElse
      ignore("learningsteps") orElse
      ignore("externalId") orElse
      ignore("revision")
  )

  val jsonEncoder = DefaultFormats ++ jsonSerializer

  override val tableName = "learningpaths"
  override val schemaName = Some(LearningpathApiProperties.MetaSchema)

  def apply(lp: SyntaxProvider[LearningPath])(rs: WrappedResultSet): LearningPath = apply(lp.resultName)(rs)

  def apply(lp: ResultName[LearningPath])(rs: WrappedResultSet): LearningPath = {
    implicit val formats = jsonEncoder
    val meta = read[LearningPath](rs.string(lp.c("document")))
    meta.copy(
      id = Some(rs.long(lp.c("id"))),
      revision = Some(rs.int(lp.c("revision"))),
      externalId = rs.stringOpt(lp.c("external_id"))
    )
  }

}
