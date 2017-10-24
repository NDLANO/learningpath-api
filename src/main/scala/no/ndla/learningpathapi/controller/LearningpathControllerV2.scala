/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpath.controller.NdlaController
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra.{Ok, ScalatraServlet}

import scala.util.Try
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain.{Language, LearningPathStatus, Sort, StepStatus}
import no.ndla.learningpathapi.service.search.SearchServiceComponent
import no.ndla.learningpathapi.service.{ConverterServiceComponent, ReadServiceComponent, UpdateServiceComponent}
import no.ndla.learningpathapi.validation.LanguageValidator
import no.ndla.network.{AuthUser}
import no.ndla.mapping
import no.ndla.mapping.LicenseDefinition

trait LearningpathControllerV2 {

  this: ReadServiceComponent with UpdateServiceComponent with SearchServiceComponent with LanguageValidator with ConverterServiceComponent =>
  val learningpathControllerV2: LearningpathControllerV2

  class LearningpathControllerV2(implicit val swagger: Swagger) extends NdlaController with ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging with CorrelationIdSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "API for accessing Learningpaths from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access not granted", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))
    val response502 = ResponseMessage(502, "Remote error", Some("Error"))

    val getLearningpaths =
      (apiOperation[SearchResultV2]("getLearningpaths")
        summary "Show public learningpaths"
        notes "Show public learningpaths."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        queryParam[Option[String]]("query").description("Return only Learningpaths with content matching the specified query."),
        queryParam[Option[String]]("tag").description("Return only Learningpaths that are tagged with this exact tag."),
        queryParam[Option[String]]("ids").description("Return only Learningpaths that have one of the provided ids. To provide multiple ids, separate by comma (,)."),
        queryParam[Option[String]]("language").description("The chosen language. Default is 'nb'"),
        queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
        queryParam[Option[Int]]("page-size").description(s"The number of search hits to display for each page. Default is ${LearningpathApiProperties.DefaultPageSize}. Max page-size is ${LearningpathApiProperties.MaxPageSize}"),
        queryParam[Option[String]]("sort").description(
          """The sorting used on results.
           Default is by -relevance (desc) when querying.
           When browsing, the default is title (asc).
           The following are supported: relevance, -relevance, lastUpdated, -lastUpdated, duration, -duration, title, -title, id, -id""".stripMargin))
        responseMessages(response400, response500)
        authorizations "oauth2")

    val getLearningpathsPost =
      (apiOperation[List[SearchResult]]("searchArticles")
        summary "Show all articles"
        notes "Shows all articles. You can search it too."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id"),
        headerParam[Option[String]]("app-key").description("Your app-key"),
        bodyParam[SearchParams]
      )
        authorizations "oauth2"
        responseMessages(response400, response500))

    val getLicenses =
      (apiOperation[List[License]]("getLicenses")
        summary "Show all valid licenses"
        notes "Shows all valid licenses"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        queryParam[Option[String]]("filter").description("A filter on the license keys. May be omitted"))
        responseMessages(response403, response500)
        authorizations "oauth2")

    val getMyLearningpaths =
      (apiOperation[List[LearningPathSummaryV2]]("getMyLearningpaths")
        summary "Show your learningpaths"
        notes "Shows your learningpaths."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."))
        responseMessages(response403, response500)
        authorizations "oauth2")

    val getLearningpath =
      (apiOperation[LearningPathV2]("getLearningpath")
        summary "Show details about the specified learningpath"
        notes "Shows all information about the specified learningpath."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        queryParam[Option[String]]("language").description("The chosen language of learningpath. Default is 'nb'"))
        responseMessages(response403, response404, response500)
        authorizations "oauth2")

    val getLearningpathStatus =
      (apiOperation[LearningPathStatus]("getLearningpathStatus")
        summary "Show publishingstatus for the learningpath"
        notes "Shows publishingstatus for the learningpath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500)
        authorizations "oauth2")

    val getLearningStepStatus =
      (apiOperation[LearningStepStatus]("getLearningStepStatus")
        summary "Show status for the learningstep"
        notes "Shows status for the learningstep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."))
        responseMessages(response403, response404, response500)
        authorizations "oauth2")

    val getLearningsteps =
      (apiOperation[List[LearningStepSummaryV2]]("getLearningsteps")
        summary "Show all learningsteps for given learningpath id"
        notes "Show all learningsteps for given learningpath id"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500)
        authorizations "oauth2")

    val getLearningStepsInTrash =
      (apiOperation[List[LearningStepSummaryV2]]("getLearningStepsInTrash")
        summary "Show all learningsteps for the given learningpath that are marked as deleted"
        notes "Show all learningsteps for the given learningpath that are marked as deleted"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500)
        authorizations "oauth2")

    val getLearningstep =
      (apiOperation[LearningStepV2]("getLearningstep")
        summary "Show the given learningstep for the given learningpath"
        notes "Show the given learningstep for the given learningpath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."),
        queryParam[Option[String]]("language").description("The chosen language of learningstep. Default is 'nb'"))
        responseMessages(response403, response404, response500, response502)
        authorizations "oauth2")

    val addNewLearningpath =
      (apiOperation[LearningPathV2]("addLearningpath")
        summary "Adds the given learningpath"
        notes "Adds the given learningpath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        bodyParam[NewLearningPathV2])
        responseMessages(response400, response403, response404, response500)
        authorizations "oauth2")

    val copyLearningpath =
      (apiOperation[LearningPathV2]("copyLearningpath")
        summary "Copies the given learningpath"
        notes "Copies the given learningpath, with the option to override some fields"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath to copy."),
        bodyParam[NewCopyLearningPathV2])
        responseMessages(response400, response403, response404, response500)
        authorizations "oauth2")

    val addNewLearningStep =
      (apiOperation[LearningStepV2]("addLearningStep")
        summary "Adds the given LearningStep"
        notes "Adds the given LearningStep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        bodyParam[NewLearningStepV2])
        responseMessages(response400, response403, response404, response500, response502)
        authorizations "oauth2")

    val updateLearningPath =
      (apiOperation[LearningPathV2]("updateLearningPath")
        summary "Update the given learningpath"
        notes "Updates the given learningPath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        bodyParam[UpdatedLearningPathV2])
        responseMessages(response400, response403, response404, response500)
        authorizations "oauth2")

    val updateLearningStep =
      (apiOperation[LearningStepV2]("updateLearningStep")
        summary "Updates the given learningStep"
        notes "Update the given learningStep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."),
        bodyParam[UpdatedLearningStepV2])
        responseMessages(response400, response403, response404, response500, response502)
        authorizations "oauth2")

    val updateLearningstepSeqNo =
      (apiOperation[LearningStepSeqNo]("updatetLearningstepSeqNo")
        summary "Update the learning step sequence number."
        notes "Updates the sequence number for the given learningstep. The sequence number of other learningsteps will be affected by this."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."),
        bodyParam[LearningStepSeqNo])
        responseMessages(response403, response404, response500)
        authorizations "oauth2")

    val updateLearningPathStatus =
      (apiOperation[LearningPathStatus]("updateLearningPathStatus")
        summary "Updates the status of the given learningPath"
        notes "Updates the status of the given learningPath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        bodyParam[LearningPathStatus])
        responseMessages(response400, response403, response404, response500)
        authorizations "oauth2")

    val updateLearningStepStatus =
      (apiOperation[LearningStepV2]("updateLearningStepStatus")
        summary "Updates the status of the given learningstep"
        notes "Updates the status of the given learningstep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."),
        bodyParam[LearningStepStatus])
        responseMessages(response400, response403, response404, response500)
        authorizations "oauth2")

    val deleteLearningPath =
      (apiOperation[LearningPathV2]("deleteLearningPath")
        summary "Deletes the given learningPath"
        notes "Deletes the given learningPath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500)
        authorizations "oauth2")

    val deleteLearningStep =
      (apiOperation[Void]("deleteLearningStep")
        summary "Deletes the given learningStep"
        notes "Deletes the given learningStep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."))
        responseMessages(response403, response404, response500)
        authorizations "oauth2")

    val getTags =
      (apiOperation[List[LearningPathTags]]("getTags")
        summary "Retrieves a list of all previously used tags in learningpaths"
        notes "Retrieves a list of all previously used tags in learningpaths"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."))
        responseMessages response500
        authorizations "oauth2")

    val getContributors =
      (apiOperation[List[Author]]("getContributors")
        summary "Retrieves a list of all previously used contributors in learningpaths"
        notes "Retrieves a list of all previously used contributors in learningpaths"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."))
        responseMessages response500
        authorizations "oauth2")

    def search(query: Option[String], language: Option[String], tag: Option[String], idList: List[Long], sort: Option[String], pageSize: Option[Int], page: Option[Int]): SearchResultV2 = {
      val searchResult = query match {
        case Some(q) => searchService.matchingQuery(
          query = q,
          withIdIn = idList,
          taggedWith = tag,
          language = language,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
          page = page,
          pageSize = pageSize
        )
        case None => searchService.allV2(
          withIdIn = idList,
          taggedWith = tag,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc),
          language = language,
          page = page,
          pageSize = pageSize)
      }

      val hitResult = searchService.getHitsV2(searchResult.response, language.getOrElse(Language.DefaultLanguage))
      
      SearchResultV2(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        hitResult
      )
    }

    get("/", operation(getLearningpaths)) {
      val query = paramOrNone("query")
      val tag = paramOrNone("tag")
      val idList = paramAsListOfLong("ids")
      val language = paramOrNone("language")
      val sort = paramOrNone("sort")
      val pageSize = paramOrNone("page-size").flatMap(ps => Try(ps.toInt).toOption)
      val page = paramOrNone("page").flatMap(idx => Try(idx.toInt).toOption)
      logger.info("GET / with params query='{}', language={}, tag={}, page={}, page-size={}, sort={}, ids={}", query, language, tag, page, pageSize, sort, idList)

      search(query, language, tag, idList, sort, pageSize, page)
    }

    post("/search/", operation(getLearningpathsPost)) {
      val searchParams = extract[SearchParams](request.body)

      val query = searchParams.query
      val tag = searchParams.tag
      val idList = searchParams.ids
      val language = searchParams.language
      val sort = searchParams.sort
      val pageSize = searchParams.pageSize
      val page = searchParams.page
      logger.info("POST /search with params query='{}', language={}, tag={}, page={}, page-size={}, sort={}, ids={}", query, language, tag, page, pageSize, sort, idList)

      search(query, language, tag, idList, sort, pageSize, page)
    }


    get("/:path_id/?", operation(getLearningpath)) {
      val language = paramOrDefault("language", Language.AllLanguages)

      readService.withIdV2(long("path_id"), language, AuthUser.get) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} and language $language not found"))
      }
    }

    get("/:path_id/status/?", operation(getLearningpathStatus)) {
      readService.statusFor(long("path_id"), AuthUser.get) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} and language $language not found"))
      }
    }

    get("/:path_id/learningsteps/?", operation(getLearningsteps)) {
      val language = paramOrDefault("language", Language.AllLanguages)

      readService.learningstepsForWithStatusV2(long("path_id"), StepStatus.ACTIVE, AuthUser.get, language) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} and language $language not found"))
      }
    }

    get("/:path_id/learningsteps/:step_id/?", operation(getLearningstep)) {
      val language = paramOrDefault("language", Language.AllLanguages)

      readService.learningstepV2For(long("path_id"), long("step_id"), language, AuthUser.get) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")} and language $language"))
      }
    }

    get("/:path_id/learningsteps/trash/?", operation(getLearningStepsInTrash)) {
      val language = paramOrDefault("language", Language.AllLanguages)

      readService.learningstepsForWithStatusV2(long("path_id"), StepStatus.DELETED, Some(requireUser), language) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      }
    }

    get("/:path_id/learningsteps/:step_id/status/?", operation(getLearningStepStatus)) {
      readService.learningStepStatusForV2(long("path_id"), long("step_id"), Language.DefaultLanguage, AuthUser.get) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
      }
    }

    get("/mine/?", operation(getMyLearningpaths)) {
      val language = paramOrDefault("language", Language.AllLanguages)
      val myLearningpaths = readService.withOwnerV2(owner = requireUser, language)

      if (myLearningpaths.isEmpty) {
        halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with language $language not found"))
      } else {
        myLearningpaths
      }
    }

    get("/licenses", operation(getLicenses)) {
      val licenses: Seq[LicenseDefinition] = paramOrNone("filter") match {
        case None => mapping.License.getLicenses
        case Some(filter) => mapping.License.getLicenses.filter(_.license.contains(filter))
      }

      licenses.map(x => License(x.license, Option(x.description), x.url))
    }

    post("/", operation(addNewLearningpath)) {
      val newLearningPath = extract[NewLearningPathV2](request.body)
      updateService.addLearningPathV2(newLearningPath, requireUser) match {
        case None => halt(status = 404, body = Error(Error.GENERIC, s"The chosen language is not supported"))
        case Some(learningPath) => {
          logger.info(s"CREATED LearningPath with ID =  ${learningPath.id}")
          halt(status = 201, headers = Map("Location" -> learningPath.metaUrl), body = learningPath)
        }
      }
    }

    post("/:path_id/copy", operation(copyLearningpath)) {
      val newLearningPath = extract[NewCopyLearningPathV2](request.body)
      val pathId = long("path_id")
      updateService.newFromExistingV2(pathId, newLearningPath, requireUser) match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id $pathId not found"))
        case Some(learningPath) => {
          logger.info(s"COPIED LearningPath with ID =  ${learningPath.id}")
          halt(status = 201, headers = Map("Location" -> learningPath.metaUrl), body = learningPath)
        }
      }
    }

    patch("/:path_id/?", operation(updateLearningPath)) {
      val updatedLearningPath = updateService.updateLearningPathV2(long("path_id"), extract[UpdatedLearningPathV2](request.body), requireUser)
      updatedLearningPath match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
        case Some(learningPath) => {
          logger.info(s"UPDATED LearningPath with ID =  ${learningPath.id}")
          Ok(body = learningPath)
        }
      }
    }

    post("/:path_id/learningsteps/?", operation(addNewLearningStep)) {
      val newLearningStep = extract[NewLearningStepV2](request.body)
      val createdLearningStep = updateService.addLearningStepV2(long("path_id"), newLearningStep, requireUser)
      createdLearningStep match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
        case Some(learningStep) => {
          logger.info(s"CREATED LearningStep with ID =  ${learningStep.id} for LearningPath with ID = ${params("path_id")}")
          halt(status = 201, headers = Map("Location" -> learningStep.metaUrl), body = createdLearningStep)
        }
      }
    }

    patch("/:path_id/learningsteps/:step_id/?", operation(updateLearningStep)) {
      val updatedLearningStep = extract[UpdatedLearningStepV2](request.body)
      val createdLearningStep = updateService.updateLearningStepV2(long("path_id"), long("step_id"),
        updatedLearningStep,
        requireUser)

      createdLearningStep match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} for learningpath with id ${params("path_id")} not found"))
        case Some(learningStep) => {
          logger.info(s"UPDATED LearningStep with ID = ${params("step_id")} for LearningPath with ID = ${params("path_id")}")
          Ok(body = learningStep)
        }
      }
    }

    put("/:path_id/learningsteps/:step_id/seqNo/?", operation(updateLearningstepSeqNo)) {
      val newSeqNo = extract[LearningStepSeqNo](request.body)
      updateService.updateSeqNo(long("path_id"), long("step_id"), newSeqNo.seqNo, requireUser) match {
        case Some(seqNo) => seqNo
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
      }
    }

    put("/:path_id/learningsteps/:step_id/status/?", operation(updateLearningStepStatus)) {
      val learningStepStatus = extract[LearningStepStatus](request.body)
      val stepStatus = StepStatus.valueOfOrError(learningStepStatus.status)

      val updatedStep = updateService.updateLearningStepStatusV2(long("path_id"), long("step_id"), stepStatus, requireUser)
      updatedStep match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} for learningpath with id ${params("path_id")} not found"))
        case Some(learningStep) => {
          logger.info(s"UPDATED LearningStep with id: ${params("step_id")} for LearningPath with id: ${params("path_id")} to STATUS = ${learningStep.status}")
          Ok(body = learningStep)
        }
      }
    }

    put("/:path_id/status/?", operation(updateLearningPathStatus)) {
      val learningPathStatus = extract[LearningPathStatus](request.body)
      val pathStatus = LearningPathStatus.valueOfOrError(learningPathStatus.status)

      val updatedLearningPath: Option[LearningPathV2] = updateService.updateLearningPathStatusV2(
        long("path_id"),
        pathStatus,
        requireUser,
        Language.DefaultLanguage)

      updatedLearningPath match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
        case Some(learningPath) => {
          logger.info(s"UPDATED status of LearningPath with ID = ${learningPath.id}")
          Ok(body = learningPath)
        }
      }
    }

    delete("/:path_id/?", operation(deleteLearningPath)) {
      val deleted = updateService.updateLearningPathStatusV2(long("path_id"), LearningPathStatus.DELETED, requireUser, Language.DefaultLanguage)
      deleted match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
        case Some(learningPath) => {
          logger.info(s"MARKED LearningPath with ID: ${params("path_id")} as DELETED")
          halt(status = 204)
        }
      }
    }

    delete("/:path_id/learningsteps/:step_id/?", operation(deleteLearningStep)) {
      val deleted = updateService.updateLearningStepStatusV2(long("path_id"), long("step_id"), StepStatus.DELETED, requireUser)
      deleted match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} for learningpath with id ${params("path_id")} not found"))
        case Some(learningStep) => {
          logger.info(s"MARKED LearningStep with id: ${params("step_id")} for LearningPath with id: ${params("path_id")} as DELETED.")
          halt(status = 204)
        }
      }
    }

    get("/tags/?", operation(getTags)) {
      val language = paramOrDefault("language", Language.AllLanguages)
      val allTags = readService.tags

      converterService.asApiLearningPathTagsSummary(allTags, language) match {
        case Some(s) => s
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Tags with language '$language' not found"))
      }
    }

    get("/contributors", operation(getContributors)) {
      readService.contributors
    }

  }

}