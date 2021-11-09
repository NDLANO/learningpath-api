/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.{LearningpathApiProperties, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatra.test.scalatest.ScalatraFunSuite
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val jsonFormats = org.json4s.DefaultFormats

  lazy val controller = new InternController
  addServlet(controller, "/*")

  test("that id with value 404 gives OK") {
    resetMocks()
    when(learningPathRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(404L))

    get("/id/1234") {
      status should equal(200)
    }
  }

  test("That DELETE /index removes all indexes") {
    reset(searchIndexService)
    when(searchIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(200)
      body should equal("Deleted 3 indexes")
    }
    verify(searchIndexService).findAllIndexes(LearningpathApiProperties.SearchIndex)
    verify(searchIndexService).deleteIndexWithName(Some("index1"))
    verify(searchIndexService).deleteIndexWithName(Some("index2"))
    verify(searchIndexService).deleteIndexWithName(Some("index3"))
    verifyNoMoreInteractions(searchIndexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    reset(searchIndexService)
    doReturn(Failure(new RuntimeException("Failed to find indexes")), Nil: _*)
      .when(searchIndexService)
      .findAllIndexes(LearningpathApiProperties.SearchIndex)
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(500)
      body should equal("Failed to find indexes")
    }
    verify(searchIndexService, never).deleteIndexWithName(any[Option[String]])
  }

  test(
    "That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless") {
    reset(searchIndexService)
    when(searchIndexService.findAllIndexes(any[String])).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists")), Nil: _*)
      .when(searchIndexService)
      .deleteIndexWithName(Some("index2"))
    doReturn(Success(""), Nil: _*).when(searchIndexService).deleteIndexWithName(Some("index3"))
    delete("/index") {
      status should equal(500)
      body should equal(
        "Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully.")
    }
    verify(searchIndexService).deleteIndexWithName(Some("index1"))
    verify(searchIndexService).deleteIndexWithName(Some("index2"))
    verify(searchIndexService).deleteIndexWithName(Some("index3"))
  }
}
