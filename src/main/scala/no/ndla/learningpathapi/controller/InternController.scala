package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.ComponentRegistry
import no.ndla.learningpathapi.model.api.Error
import no.ndla.learningpathapi.service.ImportServiceComponent
import no.ndla.learningpathapi.service.search.SearchIndexBuilderServiceComponent
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}

import scala.util.{Failure, Success}

trait InternController {
  this: ImportServiceComponent with SearchIndexBuilderServiceComponent =>
  val internController: InternController

  class InternController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
      ApplicationUrl.set(request)
    }

    after() {
      LoggerContext.clearCorrelationID
      ApplicationUrl.clear
    }

    error {
      case t: Throwable => {
        val error = Error(Error.GENERIC, t.getMessage)
        logger.error(error.toString, t)
        halt(status = 500, body = error)
      }
    }

    post("/index") {
      Ok(ComponentRegistry.searchIndexBuilderService.indexDocuments())
    }

    post("/import/:node_id") {
      val start = System.currentTimeMillis
      val nodeId = params("node_id")

      importService.doImport(nodeId) match {
        case Success(learningPathSummary) => learningPathSummary
        case Failure(ex: Throwable) => {
          val errMsg = s"Import of node with external_id $nodeId failed after ${System.currentTimeMillis - start} ms with error: ${ex.getMessage}\n"
          logger.warn(errMsg, ex)
          halt(status = 500, body = errMsg)
        }
      }
    }
  }

}
