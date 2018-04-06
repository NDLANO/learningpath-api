/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import javax.servlet.http.HttpServletRequest
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model.api.{Error, ImportReport}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.{ImportService, ReadServiceComponent}
import no.ndla.learningpathapi.service.search.SearchIndexServiceComponent
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra._

import scala.util.{Failure, Success}


trait InternController {
  this: ImportService
    with SearchIndexServiceComponent
    with LearningPathRepositoryComponent
    with ReadServiceComponent =>
  val internController: InternController

  class InternController extends NdlaController {
    protected implicit override val jsonFormats: Formats =
      org.json4s.DefaultFormats +
        new EnumNameSerializer(LearningPathStatus) +
        new EnumNameSerializer(LearningPathVerificationStatus) +
        new EnumNameSerializer(StepType) +
        new EnumNameSerializer(StepStatus) +
        new EnumNameSerializer(EmbedType)

    def requireClientId(implicit request: HttpServletRequest): String = {
      AuthUser.getClientId match {
        case Some(clientId) => clientId
        case None => {
          logger.warn(s"Request made to ${request.getRequestURI} without clientId")
          throw new AccessDeniedException("You do not have access to the requested resource.")
        }
      }
    }

    get("/id/:external_id") {
      val externalId = params("external_id")
      learningPathRepository.getIdFromExternalId(externalId) match {
        case Some(id) => id
        case None => NotFound()
      }
    }

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) =>
          val result = s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        case Failure(f) =>
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
      }
    }

    post("/import/:node_id") {
      importService.doImport(params("node_id"), requireClientId) match {
        case Success(report) => report
        case Failure(ex: ImportReport) => errorHandler(ex)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get("/dump/learningpath") {
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getLearningPathDomainDump(pageNo, pageSize)
    }

  }
}
