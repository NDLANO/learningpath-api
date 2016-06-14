package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.ComponentRegistry
import no.ndla.learningpathapi.model.api.Error
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}

trait AdminController {
  val adminController: AdminController

  class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      Ok(ComponentRegistry.searchIndexBuilderService.indexDocuments())
    }

    error {
      case t: Throwable => {
        logger.error(t.getMessage, t)
        halt(status = 500, body = Error(description = t.getMessage))
      }
    }

  }

}
