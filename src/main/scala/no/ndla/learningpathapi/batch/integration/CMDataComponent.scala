package no.ndla.learningpathapi.batch.integration

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource
import no.ndla.learningpathapi.batch.{Node, Package, Step}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool, NamedDB, _}
import com.netaporter.uri.dsl._

trait CMDataComponent {
  val cmData: CMData

  class CMData(cmHost:Option[String], cmPort:Option[String], cmDatabase:Option[String], cmUser:Option[String], cmPassword:Option[String]){
    val host = cmHost.getOrElse(throw new RuntimeException("Missing host"))
    val port = cmPort.getOrElse(throw new RuntimeException("Missing host"))
    val database = cmDatabase.getOrElse(throw new RuntimeException("Missing database"))
    val user = cmUser.getOrElse(throw new RuntimeException("Missing user"))
    val password = cmPassword.getOrElse(throw new RuntimeException("Missing password"))

    Class.forName("com.mysql.jdbc.Driver")

    val cmDatasource = new MysqlConnectionPoolDataSource
    cmDatasource.setPassword(password)
    cmDatasource.setUser(user)
    cmDatasource.setUrl(s"jdbc:mysql://$host:$port/$database")

    ConnectionPool.add('cm, new DataSourceConnectionPool(cmDatasource))

    def allLearningPaths(): List[Node] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
            select n.nid as nid, n.tnid as tnid, n.language as lang ,
            n.title as title, replace(np.url, 'http://sti.ndla.no/package/', '') as packageId,
            img.field_ingress_bilde_nid as imgnid, besk.field_ingress_value as description
            from node n
            left join ndla_packages np on n.nid = np.nid and n.vid = np.vid
            left join content_field_ingress_bilde img on n.nid = img.nid and n.vid = img.vid
            left join content_field_ingress besk on n.nid = besk.nid and n.vid = besk.vid
            where n.type = 'package'
        """.stripMargin
          .map(rs =>
            Node(
              rs.int("nid").toLong,
              rs.int("tnid").toLong,
              rs.string("lang"),
              rs.string("title"),
              rs.string("packageId").toLong,
              rs.intOpt("imgnid"),
              rs.string("description"))).list().apply()
      }
    }

    def licenseForStep(step: Step): Option[String] = {
      val nodeIdPattern = "/node/(.*)$".r
      val nodeId = step.embedUrl.getOrElse("").path match {
        case nodeIdPattern(id) => id
        case _ => return None
      }

      NamedDB('cm) readOnly { implicit session =>
        sql"""
          select license from creativecommons_lite where nid=$nodeId
        """.stripMargin.map(rs => rs.string("license")).single.apply()
      }
    }

    def imagePathForNid(imageNid: Option[Int], environment: String): Option[String] = {
      val ThumbUrlPrefix = environment match {
        case "prod" => "http://api.ndla.no/images/thumbs/"
        case _ => s"http://api.$environment.ndla.no/images/thumbs/"
      }

      imageNid match {
        case None => None
        case Some(nid) => {
          NamedDB('cm) readOnly { implicit session =>
            sql"""
            select REPLACE(f.filepath, 'sites/default/files/images/', '') as original
             from image i
             left join files f on i.fid = f.fid
             where image_size = '_original'
             and nid = $nid
        """.stripMargin
              .map(rs => ThumbUrlPrefix + rs.string("original")).single().apply()
          }
        }
      }
    }
  }

}
