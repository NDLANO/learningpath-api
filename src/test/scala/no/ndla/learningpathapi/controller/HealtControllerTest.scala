/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import org.scalatra.test.scalatest.ScalatraFunSuite

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controller = new HealthController
  addServlet(controller, "/*")

  test("That /health returns 204 no content") {
    get("/") {
      status should equal (204)
    }
  }

}