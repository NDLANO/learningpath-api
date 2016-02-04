package no.ndla.learningpathapi.model

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class LearningPath(id: Option[Long], title: List[Title], description: List[Description], coverPhotoUrl: Option[String],
                            duration: Int, status: String, verificationStatus: String, lastUpdated: Date, tags: List[LearningPathTag],
                            owner: String, learningsteps: Seq[LearningStep] = Nil) {
  def isPrivate: Boolean = {
    status == LearningpathApiProperties.Private
  }

  def isPublished: Boolean = {
    status == LearningpathApiProperties.Published
  }

  def verifyAccess(loggedInUser: Option[String]) = {
    val accessGranted = loggedInUser match {
      case None => isPublished
      case Some(user) => isPublished || user == owner
    }

    if (!accessGranted) {
      throw new AccessDeniedException("You do not have access to the requested resource.")
    }
  }
}

object LearningPath extends SQLSyntaxSupport[LearningPath] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "learningpaths"
  override val schemaName = Some(LearningpathApiProperties.MetaSchema)

  def apply(lp: SyntaxProvider[LearningPath])(rs:WrappedResultSet): LearningPath = apply(lp.resultName)(rs)
  def apply(lp: ResultName[LearningPath])(rs: WrappedResultSet): LearningPath = {
    val meta = read[LearningPath](rs.string(lp.c("document")))
    LearningPath(
      Some(rs.long(lp.c("id"))), meta.title, meta.description, meta.coverPhotoUrl, meta.duration,
      meta.status, meta.verificationStatus, meta.lastUpdated, meta.tags, meta.owner)
  }

  val JSonSerializer = FieldSerializer[LearningPath](
    ignore("id") orElse
    ignore("learningsteps")
  )
}

