/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

class V4__RenameTagToTags extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths.flatMap(convertTagsToNewFormat).foreach(update)
    }
  }

  def allLearningPaths(implicit session: DBSession): List[V4_DBLearningPath] = {
    sql"select id, document from learningpaths".map(rs => V4_DBLearningPath(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertTagsToNewFormat(learningPath: V4_DBLearningPath): Option[V4_DBLearningPath] = {
    val json = parse(learningPath.document)
    val tags = json \\ "tags"

    val oldTagsOpt: Option[List[V4_OldTag]] = tags.extractOpt[List[V4_OldTag]]
    oldTagsOpt match {
      case None => None
      case Some(oldTags) => {
        val newTags = oldTags.map(oldTag => V4_LearningPathTags(oldTag.tag, oldTag.language))
        Some(learningPath.copy(document = compact(render(json.replace(List("tags"), parse(write(newTags)))))))
      }
    }
  }

  def update(learningPath: V4_DBLearningPath)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(learningPath.document)

    sql"update learningpaths set document = $dataObject where id = ${learningPath.id}".update().apply
  }

}

case class V4_LearningPathTags(tags: Seq[String], language:Option[String])
case class V4_OldTag(tag: Seq[String], language:Option[String])
case class V4_DBLearningPath(id: Long, document: String)

