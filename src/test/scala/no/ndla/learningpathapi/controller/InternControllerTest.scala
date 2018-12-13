/*
 * Part of NDLA ndla.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatra.test.scalatest.ScalatraFunSuite
import scalikejdbc.DBSession

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
}
