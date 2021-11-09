/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s._
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V7__MovePublishedExternToUnlisted extends BaseJavaMigration {

  implicit val formats: DefaultFormats.type = DefaultFormats

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths
        .map {
          case (id, document) =>
            (id, updateStatus(document))
        }
        .foreach {
          case (id, document) =>
            update(id, document)
        }
    }
  }

  def allLearningPaths(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from learningpaths"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateStatus(document: String): String = {
    val oldLearningPath = parse(document)

    val newLearningPath = (oldLearningPath \ "verificationStatus").extractOpt[String] match {
      case Some(verStat) if verStat == "EXTERNAL" =>
        oldLearningPath.mapField {
          case ("status", JString(status)) if status == "PUBLISHED" =>
            "status" -> JString("UNLISTED")
          case x => x
        }
      case _ => oldLearningPath
    }

    compact(render(newLearningPath))
  }

  def update(id: Long, document: String)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningpaths set document = $dataObject where id = ${id}"
      .update()
  }

}
