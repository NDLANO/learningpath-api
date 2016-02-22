package no.ndla.learningpathapi.batch.integration

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource
import no.ndla.learningpathapi.batch.{Node, Package, Step}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool, _}

trait PackageDataComponent {
  val packageData: PackageData

  class PackageData(packageHost:Option[String], packagePort:Option[String], packageDatabase:Option[String], packageUser:Option[String], packagePassword:Option[String]) {
    val host = packageHost.getOrElse(throw new RuntimeException("Missing host"))
    val port = packagePort.getOrElse(throw new RuntimeException("Missing host"))
    val database = packageDatabase.getOrElse(throw new RuntimeException("Missing database"))
    val user = packageUser.getOrElse(throw new RuntimeException("Missing user"))
    val password = packagePassword.getOrElse(throw new RuntimeException("Missing password"))

    Class.forName("com.mysql.jdbc.Driver")

    val cmDatasource = new MysqlConnectionPoolDataSource
    cmDatasource.setPassword(password)
    cmDatasource.setUser(user)
    cmDatasource.setUrl(s"jdbc:mysql://$host:$port/$database")

    ConnectionPool.add('package, new DataSourceConnectionPool(cmDatasource))

    def packageFor(node: Node): Option[Package] = {
      NamedDB('package) readOnly { implicit session =>
        sql"""
         select
         p.id as packageId,
         p.updated_at as lastUpdated,
         pv.creator_id as packageAuthor,
         pv.title as packageTitle,
         pv.hours as durationHours,
         pv.minutes as durationMinutes
         from packages p
         left join package_versions pv on p.package_version_id = pv.id
         where p.id = ${node.packageId}
        """.stripMargin
          .map(rs =>
            Package(
              rs.int("packageId").toLong,
              rs.timestamp("lastUpdated"),
              rs.int("packageAuthor").toLong,
              rs.string("packageTitle"),
              rs.int("durationHours"),
              rs.int("durationMinutes"),
              node.language,
              node.nid
            )
          ).single().apply()
      }
    }


    def stepsForPackage(pakke: Package): List[Step] = {
      NamedDB('package)  readOnly{implicit session =>
        sql"""
         select p.id as packageId,
         pvp.page_id as pageId,
         pvp.pos as pagePosition,
         page.title as pageTitle,
         page.type as pageType,
         page.creator_id as pageAuthor,
         page.url as embedUrl,
         page.content as description
         from packages p
         left join package_versions pv on p.package_version_id = pv.id
         left join packageversions_pages pvp on pv.id = pvp.package_version_id
         left join pages page on page.id = pvp.page_id
         where p.id = ${pakke.packageId}
         order by pvp.pos
         """.stripMargin
          .map(rs =>
            Step(
              rs.int("packageId").toLong,
              rs.int("pageId").toLong,
              rs.int("pagePosition"),
              rs.string("pageTitle"),
              rs.int("pageType").toLong,
              rs.int("pageAuthor").toLong,
              Option(rs.string("embedUrl")),
              Option(rs.string("description")),
              pakke.language
            )
          ).list().apply()
      }
    }

    def stepWithPosForPackage(stepPos: Int, pakke: Package): Option[Step] = {
      NamedDB('package)  readOnly{implicit session =>
        sql"""
         select p.id as packageId,
         pvp.page_id as pageId,
         pvp.pos as pagePosition,
         page.title as pageTitle,
         page.type as pageType,
         page.creator_id as pageAuthor,
         page.url as embedUrl,
         page.content as description
         from packages p
         left join package_versions pv on p.package_version_id = pv.id
         left join packageversions_pages pvp on pv.id = pvp.package_version_id
         left join pages page on page.id = pvp.page_id
         where p.id = ${pakke.packageId} and pvp.pos = $stepPos
         order by pvp.pos
         """.stripMargin
          .map(rs =>
            Step(
              rs.int("packageId").toLong,
              rs.int("pageId").toLong,
              rs.int("pagePosition"),
              rs.string("pageTitle"),
              rs.int("pageType").toLong,
              rs.int("pageAuthor").toLong,
              Option(rs.string("embedUrl")),
              Option(rs.string("description")),
              pakke.language
            )
          ).single().apply()
      }
    }

    def getTranslationSteps(packages: List[Option[Package]], stepPos: Int): List[Step] = {
      packages.flatten.flatMap(pak => stepWithPosForPackage(stepPos, pak))
    }
  }
}
