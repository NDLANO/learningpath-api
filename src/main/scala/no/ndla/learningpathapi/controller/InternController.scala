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
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.api.ImportReport
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.{ImportService, ReadService, UpdateService}
import no.ndla.learningpathapi.service.search.{SearchIndexService, SearchService}
import no.ndla.network.AuthUser
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer
import org.scalatra._

import scala.util.{Failure, Success}

trait InternController {
  this: ImportService
    with SearchIndexService
    with SearchService
    with LearningPathRepositoryComponent
    with ReadService
    with UpdateService =>
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

    delete("/index") {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      val deleteResults = searchIndexService.findAllIndexes(LearningpathApiProperties.SearchIndex) match {
        case Failure(f) => halt(status = 500, body = f.getMessage)
        case Success(indexes) =>
          indexes.map(index => {
            logger.info(s"Deleting index $index")
            searchIndexService.deleteIndexWithName(Option(index))
          })
      }
      val (errors, successes) = deleteResults.partition(_.isFailure)
      if (errors.nonEmpty) {
        val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
          s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
          s"${pluralIndex(successes.length)} were deleted successfully."
        halt(status = 500, body = message)
      } else {
        Ok(body = s"Deleted ${pluralIndex(successes.length)}")
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

    get("/dump/learningpath/?") {
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getLearningPathDomainDump(pageNo, pageSize)
    }

    post("/dump/learningpath/?") {
      val dumpToInsert = extract[domain.LearningPath](request.body)
      updateService.insertDump(dumpToInsert)
    }

    get("/containsArticle") {
      val paths = paramAsListOfString("paths")

      searchService.containsPath(paths) match {
        case Success(result) => result.results
        case Failure(ex)     => errorHandler(ex)
      }
    }

  }
}
