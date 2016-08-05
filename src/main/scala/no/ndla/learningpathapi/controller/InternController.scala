package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.ComponentRegistry
import no.ndla.learningpathapi.model.api.Error
import no.ndla.learningpathapi.service.ImportServiceComponent
import no.ndla.learningpathapi.service.search.SearchIndexBuilderServiceComponent
import no.ndla.network.{ApplicationUrl, CorrelationID}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}
import no.ndla.learningpathapi.LearningpathApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import org.apache.logging.log4j.ThreadContext
import scala.util.{Failure, Success}
import com.typesafe.scalalogging.LazyLogging


trait InternController {
  this: ImportServiceComponent with SearchIndexBuilderServiceComponent =>
  val internController: InternController

  class InternController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")

      CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
      ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
      ApplicationUrl.set(request)
    }

    after() {
      CorrelationID.clear()
      ThreadContext.remove(CorrelationIdKey)
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

    post("/import") {
      val start = System.currentTimeMillis
      importService.importAll match {
        case Success(importReport) => importReport
        case Failure(ex: Throwable) => {
          val errMsg = s"Import of learningpaths failed after ${System.currentTimeMillis - start} ms with error: ${ex.getMessage}\n"
          logger.warn(errMsg, ex)
          halt(status = 500, body = errMsg)
        }
      }
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
