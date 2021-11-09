/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import io.lemonlabs.uri.dsl._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

class V3__ConvertCoverPhotoUrlToID extends BaseJavaMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths.flatMap(convertCoverPhotoUrl).foreach(update)
    }
  }

  def allLearningPaths(implicit session: DBSession): List[V3_DBLearningPath] = {
    sql"select id, document from learningpaths"
      .map(rs => V3_DBLearningPath(rs.long("id"), rs.string("document")))
      .list()
  }

  def convertCoverPhotoUrl(learningPath: V3_DBLearningPath): Option[V3_DBLearningPath] = {
    val oldDocument = parse(learningPath.document)

    oldDocument.extractOpt[V3_DBCoverPhoto] match {
      case None => None
      case Some(_) =>
        val updatedDocument = oldDocument mapField {
          case ("coverPhotoMetaUrl", JString(oldCoverPhotoUrl)) =>
            ("coverPhotoId", JString(extractImageId(oldCoverPhotoUrl)))
          case x => x
        }
        Some(learningPath.copy(document = compact(render(updatedDocument))))
    }
  }

  private def extractImageId(oldCoverPhotoUrl: String): String = {
    val pattern = """.*/images/(\d+)""".r
    pattern.findFirstMatchIn(oldCoverPhotoUrl.path.toString).map(_.group(1)).get
  }

  def update(learningPath: V3_DBLearningPath)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(learningPath.document)

    sql"update learningpaths set document = $dataObject where id = ${learningPath.id}"
      .update()
  }
}

case class V3_DBLearningPath(id: Long, document: String)
case class V3_DBCoverPhoto(coverPhotoMetaUrl: String)
