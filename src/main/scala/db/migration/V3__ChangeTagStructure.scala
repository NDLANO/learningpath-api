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

  case class OldTag(tag: String, language: Option[String])

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection): Unit = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      val all = sql"select id, document from learningpaths".map(rs => (rs.long("id"), rs.string("document"))).list().apply()
      all.foreach(tuppel => {
        val json = parse(tuppel._2)
        val tags = json \\ "tags"

        val oldTagsOpt: Option[List[OldTag]] = tags.extractOpt[List[OldTag]]
        oldTagsOpt.foreach(oldTags => {
          val newTags = oldTags.groupBy(_.language).map(entry => (entry._1, entry._2.map(_.tag))).map(entr => LearningPathTags(entr._2, entr._1))

          val newJson = json.replace(List("tags"), parse(write(newTags)))

          val dataObject = new PGobject()
          dataObject.setType("jsonb")
          dataObject.setValue(compact(render(newJson)))

          sql"update learningpaths set document = $dataObject where id = ${tuppel._1}".update().apply
        })
      })
    }
  }
}
