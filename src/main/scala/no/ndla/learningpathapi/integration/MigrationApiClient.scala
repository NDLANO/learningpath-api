package no.ndla.learningpathapi.integration

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http

trait MigrationApiClient {
  this: NdlaClient =>
  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {
    private val LearningPathsEndpoint = s"${LearningpathApiProperties.MigrationHost}/learningpaths"
    private val LearningPathEndpoint = s"$LearningPathsEndpoint/:node_id"

    def getAllLearningPathIds: Try[Seq[String]] = {
      ndlaClient.fetch[Seq[String]](Http(LearningPathsEndpoint),
        Some(LearningpathApiProperties.MigrationUser), Some(LearningpathApiProperties.MigrationPassword))
    }

    def getLearningPath(nodeId: String): Try[MainPackageImport] = {
      ndlaClient.fetch[MainPackageImport](
        Http(LearningPathEndpoint.replace(":node_id", nodeId)),
        Some(LearningpathApiProperties.MigrationUser), Some(LearningpathApiProperties.MigrationPassword))
    }
  }
}

case class MainPackageImport(mainPackage: Package, translations: Seq[Package])
case class Node(nid: Long, tnid: Long, language: String, title: String, packageId: Long, imageNid: Option[Int], description: String)
case class Step(packageId: Long, pageId: Long, pos: Int, title: String, stepType: Long, pageAuthor: Long, embedUrl: Option[String], description: Option[String], language: String) {
  def embedUrlToNdlaNo:Option[String] = {
    embedUrl match {
      case None => None
      case Some(url) => {
        val parsedUri = com.netaporter.uri.Uri.parse(url)
        parsedUri.host match {
          case None => None
          case Some(host) => {
            host match {
              case h if h == "red.ndla.no" => Some(s"http://ndla.no${parsedUri.path}")
              case default => Some(url)
            }
          }
        }
      }
    }
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
