package no.ndla.learningpathapi.model

import java.util.Date

import no.ndla.learningpathapi.validation.DurationValidator
import no.ndla.learningpathapi.{ComponentRegistry, LearningpathApiProperties, ValidationMessage}
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc._

case class LearningPath(id: Option[Long], externalId: Option[String], title: List[Title], description: List[Description], coverPhotoUrl: Option[String],
                            duration: Option[Int], status: LearningPathStatus.Value, verificationStatus: LearningPathVerificationStatus.Value, lastUpdated: Date, tags: List[LearningPathTag],
                            owner: String, learningsteps: Seq[LearningStep] = Nil) {
  def isPrivate: Boolean = {
    status == LearningPathStatus.PRIVATE
  }

  def isPublished: Boolean = {
    status == LearningPathStatus.PUBLISHED
  }

  def isNotListed: Boolean = {
    status == LearningPathStatus.NOT_LISTED
  }

  def verifyOwner(loggedInUser: String) = {
    if (loggedInUser != owner) {
      throw new AccessDeniedException("You do not have access to the requested resource.")
    }
  }

  def verifyNotPrivate = {
    if(isPrivate){
      throw new AccessDeniedException("You do not have access to the requested resource.")
    }
  }

  def verifyOwnerOrPublic(loggedInUser: Option[String]) = {
    if(isPrivate) {
      loggedInUser match {
        case Some(user) => verifyOwner(user)
        case None => throw new AccessDeniedException("You do not have access to the requested resource.")
      }
    }
  }

  def validateSeqNo(seqNo: Int) = {
    if(seqNo < 0 || seqNo > learningsteps.length-1) {
      throw new ValidationException(errors = List(ValidationMessage("seqNo", s"seqNo must be between 0 and ${learningsteps.length - 1}")))
    }
  }

  def validateForPublishing() = {
    val validationResult = new DurationValidator().validateRequired(duration).toList
    validationResult.isEmpty match {
      case true => this
      case false => throw new ValidationException(errors = validationResult)
    }
  }
}

object LearningPathStatus extends Enumeration {
  val PUBLISHED, PRIVATE, NOT_LISTED = Value

  def valueOf(s:String): Option[LearningPathStatus.Value] = {
    LearningPathStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrDefault(s:String): LearningPathStatus.Value = {
    valueOf(s).getOrElse(LearningPathStatus.PRIVATE)
  }
}

object LearningPathVerificationStatus extends Enumeration {
  val EXTERNAL, CREATED_BY_NDLA, VERIFIED_BY_NDLA = Value

  def valueOf(s:String): Option[LearningPathVerificationStatus.Value] = {
    LearningPathVerificationStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrDefault(s:String): LearningPathVerificationStatus.Value = {
    valueOf(s).getOrElse(LearningPathVerificationStatus.EXTERNAL)
  }
}

object LearningPath extends SQLSyntaxSupport[LearningPath] {
  implicit val formats = org.json4s.DefaultFormats + new EnumNameSerializer(LearningPathStatus) + new EnumNameSerializer(LearningPathVerificationStatus)
  override val tableName = "learningpaths"
  override val schemaName = Some(LearningpathApiProperties.MetaSchema)

  def apply(lp: SyntaxProvider[LearningPath])(rs:WrappedResultSet): LearningPath = apply(lp.resultName)(rs)
  def apply(lp: ResultName[LearningPath])(rs: WrappedResultSet): LearningPath = {
    val meta = read[LearningPath](rs.string(lp.c("document")))
    LearningPath(
      Some(rs.long(lp.c("id"))), rs.stringOpt(lp.c("external_id")), meta.title, meta.description, meta.coverPhotoUrl, meta.duration,
      meta.status, meta.verificationStatus, meta.lastUpdated, meta.tags, meta.owner)
  }

  val JSonSerializer = FieldSerializer[LearningPath](
    ignore("id") orElse
    ignore("learningsteps") orElse
    ignore("externalId")
  )
}

