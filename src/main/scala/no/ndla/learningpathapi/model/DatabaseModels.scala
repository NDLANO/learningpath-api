package no.ndla.learningpathapi.model

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._


case class LearningPath(id:Option[Long], title: List[Title], description: List[Description], coverPhotoUrl: Option[String],
                        duration: Int, status: String, verificationStatus: String, lastUpdated: Date, tags:List[LearningPathTag],
                        owner: String, learningsteps: Seq[LearningStep] = Nil) {

  def isPrivate: Boolean = {
    status == LearningpathApiProperties.Private
  }
}


case class LearningStep(id:Option[Long], learningPathId: Option[Long], seqNo:Int, title:List[Title], description:List[Description], embedUrl:List[EmbedUrl], `type`:String, license:Option[String])
case class Title(title:String, language:Option[String])
case class EmbedUrl(url:String, language:Option[String])
case class Description(description:String, language:Option[String])
case class LearningPathTag(tag:String, language:Option[String])

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
}

object LearningStep extends SQLSyntaxSupport[LearningStep] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "learningsteps"
  override val schemaName = Some(LearningpathApiProperties.MetaSchema)

  def apply(ls: SyntaxProvider[LearningStep])(rs:WrappedResultSet): LearningStep = apply(ls.resultName)(rs)
  def apply(ls: ResultName[LearningStep])(rs: WrappedResultSet): LearningStep = {
    val meta = read[LearningStep](rs.string(ls.c("document")))
    LearningStep(Some(rs.long(ls.c("id"))), Some(rs.long(ls.c("learning_path_id"))), meta.seqNo, meta.title, meta.description, meta.embedUrl, meta.`type`, meta.license)
  }

  def opt(ls: ResultName[LearningStep])(rs: WrappedResultSet): Option[LearningStep] = rs.longOpt(ls.c("id")).map(_ => LearningStep(ls)(rs))
}

object JSONSerializers {
  val LearningPathSerializer = FieldSerializer[LearningPath](
    ignore("id") orElse
    ignore("learningsteps")
  )

  val LearningStepSerializer = FieldSerializer[LearningStep](
    ignore("id") orElse
    ignore("learningPathId")
  )
}



