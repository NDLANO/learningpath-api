/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date

import no.ndla.learningpathapi.model.domain._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.FieldSerializer.ignore
import org.json4s._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V5__AddLanguageToAll extends BaseJavaMigration {

  implicit val formats =
    org.json4s.DefaultFormats +
      FieldSerializer[V5_LearningPath](
        ignore("id") orElse
          ignore("externalId") orElse
          ignore("learningsteps") orElse
          ignore("revision")) +
      FieldSerializer[V5_LearningStep](
        ignore("id") orElse
          ignore("learningPathId") orElse
          ignore("externalId") orElse
          ignore("revision")) +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType) +
      new EnumNameSerializer(EmbedType)

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths.foreach(update)
      allLearningSteps.foreach(update)
    }
  }

  def allLearningPaths(implicit session: DBSession): Seq[V5_LearningPath] = {
    sql"select id, document from learningpaths"
      .map(rs => {
        val meta = read[V5_LearningPath](rs.string("document"))
        meta.copy(
          id = Some(rs.long("id")),
          title = meta.title.map(t => V5_Title(t.title, Some(Language.languageOrUnknown(t.language)))),
          description =
            meta.description.map(d => V5_Description(d.description, Some(Language.languageOrUnknown(d.language)))),
          tags = meta.tags.map(t => V5_LearningPathTags(t.tags, Some(Language.languageOrUnknown(t.language))))
        )
      })
      .list()
      .apply()
  }

  def allLearningSteps(implicit session: DBSession): List[V5_LearningStep] = {
    sql"select id, document from learningsteps"
      .map(rs => {
        val meta = read[V5_LearningStep](rs.string("document"))
        meta.copy(
          id = Some(rs.long("id")),
          title = meta.title.map(t => V5_Title(t.title, Some(Language.languageOrUnknown(t.language)))),
          description =
            meta.description.map(t => V5_Description(t.description, Some(Language.languageOrUnknown(t.language)))),
          embedUrl =
            meta.embedUrl.map(t => V5_EmbedUrl(t.url, Some(Language.languageOrUnknown(t.language)), t.embedType))
        )
      })
      .list()
      .apply()
  }

  def update(learningPath: V5_LearningPath)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(learningPath))

    sql"update learningpaths set document = $dataObject where id = ${learningPath.id}"
      .update()
      .apply
  }

  def update(learningStep: V5_LearningStep)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(learningStep))

    sql"update learningsteps set document = $dataObject where id = ${learningStep.id}"
      .update()
      .apply
  }
}

case class V5_LearningPath(id: Option[Long],
                           revision: Option[Int],
                           externalId: Option[String],
                           isBasedOn: Option[Long],
                           title: Seq[V5_Title],
                           description: Seq[V5_Description],
                           coverPhotoId: Option[String],
                           duration: Option[Int],
                           status: LearningPathStatus.Value,
                           verificationStatus: LearningPathVerificationStatus.Value,
                           lastUpdated: Date,
                           tags: Seq[V5_LearningPathTags],
                           owner: String,
                           copyright: Copyright,
                           learningsteps: Seq[LearningStep] = Nil)

case class V5_LearningStep(id: Option[Long],
                           revision: Option[Int],
                           externalId: Option[String],
                           learningPathId: Option[Long],
                           seqNo: Int,
                           title: Seq[V5_Title],
                           description: Seq[V5_Description],
                           embedUrl: Seq[V5_EmbedUrl],
                           `type`: StepType.Value,
                           license: Option[String],
                           showTitle: Boolean = false,
                           status: StepStatus.Value = StepStatus.ACTIVE)

case class V5_Title(title: String, language: Option[String])
case class V5_Description(description: String, language: Option[String])
case class V5_LearningPathTags(tags: Seq[String], language: Option[String])
case class V5_EmbedUrl(url: String, language: Option[String], embedType: EmbedType.Value)
