package no.ndla.learningpathapi.batch

import no.ndla.learningpathapi.PropertiesLoader
import no.ndla.learningpathapi.model.domain.LearningPathTags
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

object LearningPathUpdater {

  implicit val formats = org.json4s.DefaultFormats

  def main(args: Array[String]){
    PropertiesLoader.load()
    transformTagsToNewFormat(92, 90, 22)
  }

  def transformTagsToNewFormat(ids: Long*) = {
    case class OldTag(tag: String, language: Option[String])

    val datasource = BatchComponentRegistry.datasource
    DB localTx {implicit session =>
      val all = sql"select id, document from learningpaths".map(rs => (rs.long("id"), rs.string("document"))).list().apply()
      all.foreach(tuppel => {
        val json = parse(tuppel._2)
        val tags = json \\ "tags"

        val oldTags: List[OldTag] = tags.extract[List[OldTag]]
        val newTags = oldTags.groupBy(_.language).map(entry => (entry._1, entry._2.map(_.tag))).map(entr => LearningPathTags(entr._2, entr._1))

        val newJson = json.replace(List("tags"), parse(write(newTags)))

        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(compact(render(newJson)))

        sql"update learningpaths set document = $dataObject where id = ${tuppel._1}".update().apply

      })
    }
  }
}
