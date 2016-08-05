/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import org.scalatra.{NoContent, ScalatraServlet}

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    get("/") {
      NoContent()
    }
  }
}