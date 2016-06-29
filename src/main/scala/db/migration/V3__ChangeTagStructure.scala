package db.migration

import java.sql.Connection

import no.ndla.learningpathapi.model.domain.LearningPathTags
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

class V3__ChangeTagStructure extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allLearningPaths.flatMap(convertTagsToNewFormat).foreach(update)
    }
  }

  def allLearningPaths(implicit session: DBSession): List[DBLearningPath] = {
    sql"select id, document from learningpaths".map(rs => DBLearningPath(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertTagsToNewFormat(learningPath: DBLearningPath): Option[DBLearningPath] = {
    val json = parse(learningPath.document)
    val tags = json \\ "tags"

    val oldTagsOpt: Option[List[OldTag]] = tags.extractOpt[List[OldTag]]
    oldTagsOpt match {
      case None => None
      case Some(oldTags) => {
        val newTags = oldTags.groupBy(_.language).map(entry => (entry._1, entry._2.map(_.tag))).map(entr => LearningPathTags(entr._2, entr._1))
        Some(learningPath.copy(document = compact(render(json.replace(List("tags"), parse(write(newTags)))))))
      }
    }
  }

  def update(learningPath: DBLearningPath)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(learningPath.document)

    sql"update learningpaths set document = $dataObject where id = ${learningPath.id}".update().apply
  }
}

case class OldTag(tag: String, language: Option[String])

case class DBLearningPath(id: Long, document: String)
