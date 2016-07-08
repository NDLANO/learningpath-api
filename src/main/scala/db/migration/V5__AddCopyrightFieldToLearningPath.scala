package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import no.ndla.mapping.LicenseMapping.getLicenseDefinition
import scalikejdbc._

class V5__AddCopyrightFieldToLearningPath extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths.flatMap(addCopyrightField).foreach(update)
    }
  }

  def allLearningPaths(implicit session: DBSession): List[V5_DBLearningPath] = {
    sql"select id, document from learningpaths".map(rs => V5_DBLearningPath(rs.long("id"), rs.string("document"))).list().apply()
  }

  def addCopyrightField(learningPath: V5_DBLearningPath): Option[V5_DBLearningPath] = {
    val json = parse(learningPath.document)
    val copyright = json \\ "copyright"

    val oldCopyrightOpt: Option[V5_Copyright] = copyright.extractOpt[V5_Copyright]
    oldCopyrightOpt match {
      case None => {
        val (description, url) = getLicenseDefinition("by-sa").getOrElse(("", ""))
        val newCopyRight = parse(write(V5_Copyright(V5_License("by-sa", description, url), "", List())))
        val newCopyRightJson = parse(write(("copyright", newCopyRight)))
        Some(learningPath.copy(document=compact(render(json merge newCopyRightJson))))
      }
      case Some(oldCopyright) => None
    }
  }

  def update(learningPath: V5_DBLearningPath)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(learningPath.document)

    sql"update learningpaths set document = $dataObject where id = ${learningPath.id}".update().apply
  }
}

case class V5_DBLearningPath(id: Long, document: String)
case class V5_Copyright(license: V5_License, origin: String, contributors: Seq[V5_Author])
case class V5_License(license: String, description: String, url: String)
case class V5_Author(`type`: String, name: String)
