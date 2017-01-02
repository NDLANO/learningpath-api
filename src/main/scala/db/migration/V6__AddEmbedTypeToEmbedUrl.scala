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

class V6__AddEmbedTypeToEmbedUrl extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.flatMap(convertDocumentToNewFormat).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[V6_DBContent] = {
    sql"select id, document from learningsteps".map(rs => V6_DBContent(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertDocumentToNewFormat(learningStep: V6_DBContent): Option[V6_DBContent] = {
    val json = parse(learningStep.document)

    val embedUrl = json \\ "embedUrl"
    val oldEmbedUrl: Option[List[V6_OldEmbedUrl]] = embedUrl.extractOpt[List[V6_OldEmbedUrl]]

    oldEmbedUrl.flatMap(embed => {
      val newEmbedUrl = embed.map(oldEmbed => V6_EmbedUrl(oldEmbed.url, oldEmbed.language, "oembed"))
      Some(learningStep.copy(document = compact(render(json.replace(List("embedUrl"), parse(write(newEmbedUrl)))))))
    })
  }

  def update(content: V6_DBContent)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(content.document)

    sql"update learningsteps set document = $dataObject where id = ${content.id}".update().apply
  }

}
case class V6_DBContent(id: Long, document: String)

case class V6_EmbedUrl(url: String, language: String, embedType: String)
case class V6_OldEmbedUrl(url: String, language: String)