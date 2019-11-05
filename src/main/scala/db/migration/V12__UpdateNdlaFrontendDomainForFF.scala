/**
  * Part of NDLA ndla.
  * Copyright (C) 2019 NDLA
  *
  * See LICENSE
  */
package db.migration

import no.ndla.learningpathapi.LearningpathApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.Formats
import org.json4s.JsonAST.{JArray, JObject, JString}
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}
import io.lemonlabs.uri.Url

class V12__UpdateNdlaFrontendDomainForFF extends BaseJavaMigration {
  implicit val formats: Formats = org.json4s.DefaultFormats + new EnumNameSerializer(V11_EmbedType)

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateLearningSteps
    }
  }

  def migrateLearningSteps(implicit session: DBSession): Unit = {
    val count = countAllLearningSteps.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allLearingSteps(offset * 1000).map {
        case (id, document) => updateLearningStep(migrateLearningPathDocument(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllLearningSteps(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from learningsteps where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
      .apply()
  }

  def allLearingSteps(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from learningsteps where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list
      .apply()
  }

  def updateLearningStep(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningsteps set document = $dataObject where id = $id"
      .update()
      .apply
  }

  def updateNdlaUrl(oldUrl: String): String = {
    val parsed = Url.parse(oldUrl)
    val isNDLAHost = parsed.hostOption.exists(_.toString.contains("ndla.no"))

    if (isNDLAHost) parsed.toRelativeUrl.toString
    else oldUrl
  }

  private[migration] def migrateLearningPathDocument(document: String): String = {
    val oldLs = parse(document)

    val newLs = oldLs.mapField {
      case ("embedUrl", embedUrlArray: JArray) =>
        val updatedEmbedUrlArray = embedUrlArray.map {
          case embedUrlObj: JObject =>
            embedUrlObj.mapField {
              case ("url", JString(url)) => ("url", JString(updateNdlaUrl(url)))
              case constantEmbedUrlField => constantEmbedUrlField
            }
          case z => z // Non object in array, should not occur
        }
        ("embedUrl", updatedEmbedUrlArray)
      case constantLsField => constantLsField
    }
    compact(render(newLs))
  }
}

object V11_EmbedType extends Enumeration {
  val OEmbed = Value("oembed")
  val LTI = Value("lti")
}
case class V11_LearningStep(embedUrl: Seq[V11_EmbedUrl])
case class V11_EmbedUrl(url: String, language: String, embedType: V11_EmbedType.Value)
