/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http
import com.netaporter.uri.dsl._

trait MigrationApiClient {
  this: NdlaClient =>
  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {
    private val LearningPathsEndpoint = s"${LearningpathApiProperties.MigrationHost}/learningpaths"
    private val LearningPathEndpoint = s"$LearningPathsEndpoint/:node_id"

    def getAllLearningPathIds: Try[Seq[String]] = {
      ndlaClient.fetchWithBasicAuth[Seq[String]](Http(LearningPathsEndpoint),
        LearningpathApiProperties.MigrationUser, LearningpathApiProperties.MigrationPassword)
    }

    def getLearningPath(nodeId: String): Try[MainPackageImport] = {
      ndlaClient.fetchWithBasicAuth[MainPackageImport](
        Http(LearningPathEndpoint.replace(":node_id", nodeId)),
        LearningpathApiProperties.MigrationUser, LearningpathApiProperties.MigrationPassword)
    }
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
