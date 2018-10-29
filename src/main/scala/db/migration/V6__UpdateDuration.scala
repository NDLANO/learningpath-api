/*
 * Part of NDLA learningpath_api.
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

class V6__UpdateDuration extends BaseJavaMigration {

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths
        .map {
          case (id, document) =>
            (id, updateDuration(document))
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
      .apply()
  }

  def updateDuration(document: String): String = {
    val oldLearningPath = parse(document)

    val newLearningPath = oldLearningPath.mapField {
      case ("duration", JInt(duration)) if duration <= 0 =>
        "duration" -> JInt(1)
      case x => x
    }
    compact(render(newLearningPath))
  }

  def update(id: Long, document: String)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningpaths set document = $dataObject where id = ${id}"
      .update()
      .apply
  }

}
