/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.learningpathapi.model.domain.{EmbedType, LearningStep}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{Extraction, Formats}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V14__ConvertLanguageUnknown extends BaseJavaMigration {
  implicit val formats: Formats = LearningStep.jsonEncoder

  override def migrate(context: Context): Unit = {
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

  def convertLearningStepDocument(document: String): String = {
    val oldStep = parse(document)
    val extractedStep = oldStep.extract[V14_LearningStep]
    val embedUrl = extractedStep.embedUrl.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val title = extractedStep.title.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val desc = extractedStep.description.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val updated = oldStep
      .replace(List("embedUrl"), Extraction.decompose(embedUrl))
      .replace(List("title"), Extraction.decompose(title))
      .replace(List("description"), Extraction.decompose(desc))
    compact(render(updated))
  }

  def convertLearningPathDocument(document: String): String = {
    val oldLearningpath = parse(document)
    val extractedPath = oldLearningpath.extract[V14_LearningPath]
    val tags = extractedPath.tags.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val title = extractedPath.title.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val desc = extractedPath.description.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val updated = oldLearningpath
      .replace(List("tags"), Extraction.decompose(tags))
      .replace(List("title"), Extraction.decompose(title))
      .replace(List("description"), Extraction.decompose(desc))
    compact(render(updated))
  }

  def updateLearningPath(id: Long, document: String)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningpaths set document = $dataObject where id = $id"
      .update()
  }

  def updateLearningStep(id: Long, document: String)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningsteps set document = $dataObject where id = $id"
      .update()
  }

  case class V14_Title(title: String, language: String)
  case class V14_Description(description: String, language: String)
  case class V14_LearningPathTags(tags: Seq[String], language: String)
  case class V14_LearningPath(title: Seq[V14_Title], description: Seq[V14_Description], tags: Seq[V14_LearningPathTags])
  case class V14_EmbedUrl(url: String, language: String, embedType: EmbedType.Value)
  case class V14_LearningStep(title: Seq[V14_Title], description: Seq[V14_Description], embedUrl: Seq[V14_EmbedUrl])
}
