/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import java.util.Date

import com.netaporter.uri.dsl._
import no.ndla.learningpathapi.LearningpathApiProperties.{Environment, MigrationHost, MigrationPassword, MigrationUser}
import no.ndla.learningpathapi.caching.Memoize
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http

trait MigrationApiClient {
  this: NdlaClient =>
  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {
    val DBSource = "red"
    private val LearningPathsEndpoint = s"$MigrationHost/learningpaths" ? (s"db-source" -> s"$DBSource")
    private val LearningPathEndpoint = s"$MigrationHost/learningpaths/:node_id" ? (s"db-source" -> s"$DBSource")

    def getAllLearningPathIds: Try[Seq[String]] = {
      ndlaClient.fetchWithBasicAuth[Seq[String]](Http(LearningPathsEndpoint),
        MigrationUser, MigrationPassword)
    }

    def getLearningPath(nodeId: String): Try[MainPackageImport] = {
      ndlaClient.fetchWithBasicAuth[MainPackageImport](
        Http(LearningPathEndpoint.replace(":node_id", nodeId)),
        MigrationUser, MigrationPassword)
    }

    val getAllNodeIds: Memoize[String, Set[ArticleMigrationContent]] = Memoize((nodeId: String) => {
      val url = s"$MigrationHost/contents/$nodeId" ? ("db-source" -> s"$DBSource")
      ndlaClient.fetchWithBasicAuth[ArticleMigrationData](Http(url), MigrationUser, MigrationPassword)
        .toOption
        .map(_.contents)
        .getOrElse(Seq.empty).toSet
    })
  }
}

case class MainPackageImport(mainPackage: Package, translations: Seq[Package])
case class Node(nid: Long, tnid: Long, language: String, title: String, packageId: Long, imageNid: Option[Int], description: String)
case class Step(packageId: Long, pageId: Long, pos: Int, title: String, stepType: Long, pageAuthor: Long, embedUrl: Option[String], description: Option[String], license: Option[String], language: String) {
  def embedUrlToNdlaNo: Option[String] = {
    embedUrl.flatMap(url => url.host.map(host => {
      if (host == "red.ndla.no") {
        s"https://ndla.no${url.path}"
      } else {
        url.copy(scheme=Some("https"))
      }
    }))

  }
}
case class Package(nid: Long,
                   tnid: Long,
                   language: String,
                   title: String,
                   imageNid: Option[Int],
                   description: String,
                   packageId: Long,
                   lastUpdated: Date,
                   packageAuthor: Long,
                   packageTitle: String,
                   durationHours: Int,
                   durationMinutes: Int,
                   steps: Seq[Step])
case class ArticleMigrationContent(nid: String, tnid: String) {
  def isMainNode: Boolean = nid == tnid || tnid == "0"
}
case class ArticleMigrationData(contents: Seq[ArticleMigrationContent])
