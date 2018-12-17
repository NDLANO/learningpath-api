/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import java.util.UUID

import javax.servlet.http.HttpServletRequest
import no.ndla.learningpathapi.model.api.ImportReport
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.{ImportService, ReadService}
import no.ndla.learningpathapi.service.search.SearchIndexService
import no.ndla.network.AuthUser
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.scalatra._

import scala.util.{Failure, Success}

trait InternController {
  this: ImportService with SearchIndexService with LearningPathRepositoryComponent with ReadService =>
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
        case Some(id) => id.toString
        case None     => NotFound()
      }
    }

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) =>
          val result =
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        case Failure(f) =>
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
      }
    }

    post("/import/:node_id") {
      val importId = paramOrDefault("importId", UUID.randomUUID().toString)
      val nodeId = params("node_id")
      val clientId = requireClientId

      importService.doImport(nodeId, clientId, importId) match {
        case Success(report)           => report
        case Failure(ex: ImportReport) => errorHandler(ex)
        case Failure(ex)               => errorHandler(ex)
      }
    }

    get("/dump/learningpath") {
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getLearningPathDomainDump(pageNo, pageSize)
    }

  }
}
