package no.ndla.learningpathapi.batch.integration

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource
import no.ndla.learningpathapi.batch.Node
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool, _}


class CMData(cmHost:Option[String], cmPort:Option[String], cmDatabase:Option[String], cmUser:Option[String], cmPassword:Option[String]) {
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
            select n.nid as nid, n.tnid as tnid, n.language as lang , n.title as title, replace(np.url, 'http://sti.ndla.no/package/', '') as packageId
            from node n
            left join ndla_packages np on n.nid = np.nid and n.vid = np.vid
            where n.type = 'package'
        """.stripMargin
        .map(rs =>
          Node(
            rs.int("nid").toLong,
            rs.int("tnid").toLong,
            rs.string("lang"),
            rs.string("title"),
            rs.string("packageId").toLong)).list().apply()
    }
  }
}
