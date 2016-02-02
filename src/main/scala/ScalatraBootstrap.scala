/*
 * Part of NDLA LEARNINGPATH API. API for searching learningpaths from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */

import javax.servlet.ServletContext

import no.ndla.learningpathapi.controller.{AdminController, LearningpathController}
import no.ndla.learningpathapi.{ResourcesApp, LearningpathSwagger}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new LearningpathSwagger

  override def init(context: ServletContext) {
    context.mount(new LearningpathController, "/learningpaths", "learningpaths")
    context.mount(new ResourcesApp, "/api-docs")
    context.mount(new AdminController, "/admin")
  }
}
