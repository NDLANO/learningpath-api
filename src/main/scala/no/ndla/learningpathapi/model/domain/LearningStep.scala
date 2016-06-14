package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.LearningpathApiProperties
import org.json4s.FieldSerializer._
import org.json4s._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc._

case class LearningStep(id: Option[Long],
                        revision: Option[Int],
                        externalId: Option[String],
                        learningPathId: Option[Long],
                        seqNo: Int,
                        title: Seq[Title],
                        description: Seq[Description],
                        embedUrl: Seq[EmbedUrl],
                        `type`: StepType.Value,
                        license: Option[String],
                        showTitle: Boolean = false)

object StepType extends Enumeration {
  val INTRODUCTION, TEXT, QUIZ, TASK, MULTIMEDIA, SUMMARY, TEST = Value

  def valueOf(s: String): Option[StepType.Value] = {
    StepType.values.find(_.toString == s)
  }

  def valueOfOrDefault(s: String): StepType.Value = {
    valueOf(s).getOrElse(StepType.TEXT)
  }
}

object LearningStep extends SQLSyntaxSupport[LearningStep] {
  val JSonSerializer = FieldSerializer[LearningStep](
    serializer =
      ignore("id") orElse
      ignore("learningPathId") orElse
      ignore("externalId") orElse
      ignore("revision"))

  implicit val formats = org.json4s.DefaultFormats + new EnumNameSerializer(StepType) + JSonSerializer
  override val tableName = "learningsteps"
  override val schemaName = Some(LearningpathApiProperties.MetaSchema)

  def apply(ls: SyntaxProvider[LearningStep])(rs: WrappedResultSet): LearningStep = apply(ls.resultName)(rs)

  def apply(ls: ResultName[LearningStep])(rs: WrappedResultSet): LearningStep = {
    val meta = read[LearningStep](rs.string(ls.c("document")))
    LearningStep(Some(rs.long(ls.c("id"))), Some(rs.int(ls.c("revision"))), rs.stringOpt(ls.c("external_id")), Some(rs.long(ls.c("learning_path_id"))), meta.seqNo, meta.title, meta.description, meta.embedUrl, meta.`type`, meta.license, meta.showTitle)
  }

  def opt(ls: ResultName[LearningStep])(rs: WrappedResultSet): Option[LearningStep] = rs.longOpt(ls.c("id")).map(_ => LearningStep(ls)(rs))
}