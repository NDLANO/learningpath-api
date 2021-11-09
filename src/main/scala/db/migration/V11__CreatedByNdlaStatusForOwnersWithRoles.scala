/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.domain.LearningPathVerificationStatus
import no.ndla.network.AuthUser
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JString
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalaj.http.Http
import scalikejdbc.{DB, DBSession, _}

import scala.util.{Failure, Success, Try}

class V11__CreatedByNdlaStatusForOwnersWithRoles extends BaseJavaMigration with LazyLogging {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  private val auth0Host = AuthUser.getAuth0HostForEnv(LearningpathApiProperties.Environment)

  case class UserMetaDataObject(ndla_id: String)
  case class UserResponseObject(app_metadata: UserMetaDataObject)
  case class Auth0TokenRequestBody(grant_type: String, client_id: String, client_secret: String, audience: String)
  case class Auth0TokenResponseBody(access_token: String)

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)
    db.withinTx(implicit session => {
      getOwnerIdsWithRoles match {
        case Failure(ex) =>
          logger.error("Something went wrong during fetching users from auth0.")
          throw ex
        case Success(ownerIds) if ownerIds.size > 0 =>
          allLearningPathsWithOwnerInList(ownerIds)
            .map { case (id, document) => (id, convertLearningPathDocument(document)) }
            .foreach {
              case (id, convertedDocument) =>
                println(s"Setting the verificationStatus of $id to '${LearningPathVerificationStatus.CREATED_BY_NDLA}'")
                updateLearningPath(id, convertedDocument)
            }
        case _ => // No paths to migrate
      }
    })
  }

  def getAuth0Token: Try[String] = {
    val requestBody = for {
      client_id <- Try(LearningpathApiProperties.prop("LEARNINGPATH_CLIENT_ID"))
      client_secret <- Try(LearningpathApiProperties.prop("LEARNINGPATH_CLIENT_SECRET"))
    } yield
      Auth0TokenRequestBody(
        "client_credentials",
        client_id,
        client_secret,
        s"https://$auth0Host/api/v2/"
      )

    requestBody
      .flatMap(body => {
        Try(
          Http(s"https://$auth0Host/oauth/token")
            .header("Content-Type", "application/json")
            .postData(write(body))
            .asString
        )
      })
      .map(res =>
        if (res.code == 200) {
          read[Auth0TokenResponseBody](res.body).access_token
        } else {
          throw new RuntimeException("Could not fetch auth0 token for listing users with roles.")
      })
  }

  private def getAuth0Response(token: String, page: Int, pageSize: Int) = {
    val url = s"https://$auth0Host/api/v2/users"
    Try(
      Http(url)
        .header("Authorization", s"Bearer $token")
        .method("GET")
        .params(
          "search_engine" -> "v3",
          "q" -> """app_metadata.roles:"learningpath:write" || app_metadata.roles:"learningpath:admin" || app_metadata.roles:"learningpath:publish"""",
          "page" -> page.toString,
          "per_page" -> pageSize.toString
        )
        .asString
    ) match {
      case Failure(ex) =>
        println(s"Could not fetch users at page '$page' with page-size '$pageSize' on url '$url'.")
        Failure(ex)
      case Success(res) =>
        println(s"Successfully fetched users at page '$page' with page-size '$pageSize' on url '$url'.")
        Success(res)
    }
  }

  private def getOwnerIdsWithRolesOnPage(token: String,
                                         page: Int,
                                         results: List[String] = List.empty): Try[List[String]] = {
    val pageSize = 50
    getAuth0Response(token, page, pageSize) match {
      case Failure(ex) => Failure(ex)
      case Success(response) =>
        val parsedResponse = Try(read[List[UserResponseObject]](response.body))
        parsedResponse.map(listOfUsers => listOfUsers.map(userData => userData.app_metadata.ndla_id)) match {
          case Failure(ex)                          => Failure(ex)
          case Success(ids) if ids.size >= pageSize => getOwnerIdsWithRolesOnPage(token, page + 1, results ++ ids)
          case Success(ids)                         => Success(results ++ ids)
        }
    }
  }

  private def getOwnerIdsWithRoles(implicit session: DBSession) =
    if (learningPathCount > 0)
      getAuth0Token.flatMap(token => { getOwnerIdsWithRolesOnPage(token, 0) })
    else
      Success(List.empty)

  def learningPathCount(implicit session: DBSession): Long = {
    sql"select count(*) from learningpaths".map(rs => rs.long("count")).single().getOrElse(0)
  }

  def allLearningPathsWithOwnerInList(ownerList: List[String])(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from learningpaths where document->>'owner' in ($ownerList)"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list()
  }

  def convertLearningPathDocument(document: String): String = {
    val oldArticle = parse(document)

    val newPath = oldArticle.mapField {
      case ("verificationStatus", _: JString) =>
        "verificationStatus" -> JString(LearningPathVerificationStatus.CREATED_BY_NDLA.toString)
      case x => x
    }
    compact(render(newPath))
  }

  def updateLearningPath(id: Long, document: String)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update learningpaths set document = $dataObject where id = ${id}"
      .update()
  }

  case class V9__LearningPath(owner: String)
}
