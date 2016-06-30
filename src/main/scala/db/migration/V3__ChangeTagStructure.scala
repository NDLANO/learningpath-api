package db.migration

import java.sql.Connection

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

  def allLearningPaths(implicit session: DBSession): List[V3_DBLearningPath] = {
    sql"select id, document from learningpaths".map(rs => V3_DBLearningPath(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertTagsToNewFormat(learningPath: V3_DBLearningPath): Option[V3_DBLearningPath] = {
    val json = parse(learningPath.document)
    val tags = json \\ "tags"

    val oldTagsOpt: Option[List[V3_OldTag]] = tags.extractOpt[List[V3_OldTag]]
    oldTagsOpt match {
      case None => None
      case Some(oldTags) => {
        val newTags = oldTags.groupBy(_.language).map(entry => (entry._1, entry._2.map(_.tag))).map(entr => V3_LearningPathTags(entr._2, entr._1))
        Some(learningPath.copy(document = compact(render(json.replace(List("tags"), parse(write(newTags)))))))
      }
    }
  }

  def update(learningPath: V3_DBLearningPath)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(learningPath.document)

    sql"update learningpaths set document = $dataObject where id = ${learningPath.id}".update().apply
  }
}

case class V3_LearningPathTags(tag: Seq[String], language:Option[String])
case class V3_OldTag(tag: String, language: Option[String])
case class V3_DBLearningPath(id: Long, document: String)
