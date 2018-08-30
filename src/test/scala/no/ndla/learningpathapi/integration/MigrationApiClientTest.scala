/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import java.util.Date

import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import no.ndla.network.model.HttpRequestException
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.util.{Failure, Success}
import scalaj.http.HttpRequest
import org.scalatest.TryValues._

class MigrationApiClientTest extends UnitSuite with UnitTestEnvironment {

  override val migrationApiClient = new MigrationApiClient

  val testStep = Step(packageId = 1,
                      pageId = 1,
                      pos = 1,
                      title = "",
                      stepType = 1,
                      pageAuthor = 1,
                      embedUrl = None,
                      description = None,
                      language = "",
                      license = None)

  val testNode = Node(nid = 1, tnid = 1, language = "", title = "", packageId = 1, imageNid = None, description = "")

  val mainPackage = MainPackageImport(
    Package(1, 1, "nb", "Title", None, "Description", 1, new Date(), 1, "PackageTitle", 1, 1, Seq(), "by-sa", Seq()),
    Seq())

  test("That failure is returned when ndlaClient returns a failure") {
    val exception = new HttpRequestException("This is an error")
    when(
      ndlaClient.fetchWithBasicAuth[MainPackageImport](any[HttpRequest], any[String], any[String])(
        any[Manifest[MainPackageImport]])).thenReturn(Failure(exception))

    val result = migrationApiClient.getLearningPath("abc")
    result should be a 'failure
    result.failure.exception.getMessage should equal(exception.getMessage)
  }

  test("That a MainPackageImport is returned when ndlaClient returns a success") {
    when(
      ndlaClient.fetchWithBasicAuth[MainPackageImport](any[HttpRequest], any[String], any[String])(
        any[Manifest[MainPackageImport]])).thenReturn(Success(mainPackage))

    val result = migrationApiClient.getLearningPath("abc")
    result should be a 'success
    result.success.value should equal(mainPackage)
  }

}
