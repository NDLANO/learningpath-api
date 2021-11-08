/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import org.scalatra.{Ok, ScalatraServlet}

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    get("/") {
      Ok()
    }
  }
}
