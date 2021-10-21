/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JString
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, JObject}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V8__UpdateLicenses extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths
        .map { case (id, document) => (id, convertLearningPathDocument(document)) }
        .foreach { case (id, document) => updateLearningPath(id, document) }

      allLearningSteps
        .map { case (id, document) => (id, convertLearningStepDocument(document)) }
        .foreach { case (id, document) => updateLearningStep(id, document) }
    }
  }

  def allLearningPaths(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from learningpaths"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def allLearningSteps(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from learningsteps"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def updateLicense(license: String): String = {
    val mapping = Map(
      "by" -> "CC-BY-4.0",
      "by-sa" -> "CC-BY-SA-4.0",
      "by-nc" -> "CC-BY-NC-4.0",
      "by-nd" -> "CC-BY-ND-4.0",
      "by-nc-sa" -> "CC-BY-NC-SA-4.0",
      "by-nc-nd" -> "CC-BY-NC-ND-4.0",
      "by-3.0" -> "CC-BY-4.0",
      "by-sa-3.0" -> "CC-BY-SA-4.0",
      "by-nc-3.0" -> "CC-BY-NC-4.0",
      "by-nd-3.0" -> "CC-BY-ND-4.0",
      "by-nc-sa-3.0" -> "CC-BY-NC-SA-4.0",
      "by-nc-nd-3.0" -> "CC-BY-NC-ND-4.0",
      "cc0" -> "CC0-1.0",
      "pd" -> "PD",
      "copyrighted" -> "COPYRIGHTED"
    )

    mapping.getOrElse(license, license)
  }

  def convertLearningStepDocument(document: String): String = {
    val oldStep = parse(document)

    val newStep = oldStep.mapField {
      case ("license", license: JString) =>
        "license" -> JString(updateLicense(license.values))
      case x => x
    }
    compact(render(newStep))
  }

  def convertLearningPathDocument(document: String): String = {
    val oldArticle = parse(document)

    val newPath = oldArticle.mapField {
      case ("copyright", copyright: JObject) =>
        "copyright" -> copyright.mapField {
          case ("license", license: JString) =>
            "license" -> JString(updateLicense(license.values))
          case x => x
        }
      case x => x
    }
    compact(render(newPath))
  }

  def updateLearningPath(id: Long, document: String)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningpaths set document = $dataObject where id = ${id}"
      .update()
  }

  def updateLearningStep(id: Long, document: String)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningsteps set document = $dataObject where id = ${id}"
      .update()
  }

}
