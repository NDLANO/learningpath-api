/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain.{
  Author => _,
  LearningPathStatus => _,
  LearningPathTags => _,
  License => _,
  _
}
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.service.search.SearchServiceComponent
import no.ndla.learningpathapi.service.{ConverterService, ReadServiceComponent, UpdateService}
import no.ndla.learningpathapi.validation.LanguageValidator
import no.ndla.mapping
import no.ndla.mapping.LicenseDefinition
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger._
import org.scalatra.util.NotNothing
import org.scalatra.{BadRequest, Ok, ScalatraServlet}

import scala.util.{Failure, Success, Try}

trait LearningpathControllerV2 {

  this: ReadServiceComponent
    with UpdateService
    with SearchServiceComponent
    with LanguageValidator
    with ConverterService =>
  val learningpathControllerV2: LearningpathControllerV2

  class LearningpathControllerV2(implicit val swagger: Swagger)
      extends NdlaController
      with ScalatraServlet
      with NativeJsonSupport
      with SwaggerSupport
      with LazyLogging
      with CorrelationIdSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription =
      "API for accessing Learningpaths from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 =
      ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access not granted", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))
    val response502 = ResponseMessage(502, "Remote error", Some("Error"))

    case class Param(paramName: String, description: String)

    private val correlationId =
      Param("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query = Param("query", "Return only Learningpaths with content matching the specified query.")
    private val language =
      Param("language", "The ISO 639-1 language code describing language.")
    private val sort = Param(
      "sort",
      """The sorting used on results.
             The following are supported: relevance, -relevance, duration, -duration, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val pageNo =
      Param("page", "The page number of the search hits to display.")
    private val pageSize =
      Param("page-size", "The number of search hits to display for each page.")
    private val learningpathId =
      Param("learningpath_id", "Id of the learningpath.")
    private val learningstepId =
      Param("learningstep_id", "Id of the learningstep.")
    private val tag = Param("tag", "Return only Learningpaths that are tagged with this exact tag.")
    private val learningpathIds = Param(
      "ids",
      "Return only Learningpaths that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val licenseFilter =
      Param("filter", "Query for filtering licenses. Only licenses containing filter-string are returned.")
    private val learningPathStatus = Param("STATUS", "Status of LearningPaths")
    private val adminMessage = Param(
      "message",
      "Message that admins can place on a LearningPath for notifying a owner of issues with LearningPath")

    private def asQueryParam[T: Manifest: NotNothing](param: Param) =
      queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param) =
      headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param) =
      pathParam[T](param.paramName).description(param.description)
    private def asFormParam[T: Manifest: NotNothing](param: Param) =
      formParam[T](param.paramName).description(param.description)
    private def asFileParam(param: Param) =
      Parameter(name = param.paramName,
                `type` = ValueDataType("file"),
                description = Some(param.description),
                paramType = ParamType.Form)

    def search(query: Option[String],
               language: Option[String],
               tag: Option[String],
               idList: List[Long],
               sort: Option[String],
               pageSize: Option[Int],
               page: Option[Int]): SearchResultV2 = {
      query match {
        case Some(q) =>
          searchService.matchingQuery(
            query = q,
            withIdIn = idList,
            taggedWith = tag,
            language = language,
            sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
            page = page,
            pageSize = pageSize
          )
        case None =>
          searchService.allV2(withIdIn = idList,
                              taggedWith = tag,
                              sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc),
                              language = language,
                              page = page,
                              pageSize = pageSize)
      }
    }

    private val getLearningpaths =
      (apiOperation[SearchResultV2]("getLearningpaths")
        summary "Find public learningpaths"
        notes "Show public learningpaths."
        parameters (asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[String]](tag),
        asQueryParam[Option[String]](learningpathIds),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[Int]](pageSize),
        asQueryParam[Option[String]](sort))
        responseMessages (response400, response500)
        authorizations "oauth2")

    get("/", operation(getLearningpaths)) {
      val query = paramOrNone(this.query.paramName)
      val tag = paramOrNone(this.tag.paramName)
      val idList = paramAsListOfLong(this.learningpathIds.paramName)
      val language = paramOrNone(this.language.paramName)
      val sort = paramOrNone(this.sort.paramName)
      val pageSize = paramOrNone(this.pageSize.paramName).flatMap(ps => Try(ps.toInt).toOption)
      val page = paramOrNone(this.pageNo.paramName).flatMap(idx => Try(idx.toInt).toOption)
      logger.info("GET / with params query='{}', language={}, tag={}, page={}, page-size={}, sort={}, ids={}",
                  query,
                  language,
                  tag,
                  page,
                  pageSize,
                  sort,
                  idList)

      search(query, language, tag, idList, sort, pageSize, page)
    }

    private val getLearningpathsPost =
      (apiOperation[List[SearchResultV2]]("searchArticles")
        summary "Find public learningpaths"
        notes "Show public learningpaths"
        parameters (
          asHeaderParam[Option[String]](correlationId),
          bodyParam[SearchParams]
      )
        authorizations "oauth2"
        responseMessages (response400, response500))

    post("/search/", operation(getLearningpathsPost)) {
      val searchParams = extract[SearchParams](request.body)

      val query = searchParams.query
      val tag = searchParams.tag
      val idList = searchParams.ids
      val language = searchParams.language
      val sort = searchParams.sort
      val pageSize = searchParams.pageSize
      val page = searchParams.page
      logger.info("POST /search with params query='{}', language={}, tag={}, page={}, page-size={}, sort={}, ids={}",
                  query,
                  language,
                  tag,
                  page,
                  pageSize,
                  sort,
                  idList)

      search(query, language, tag, idList, sort, pageSize, page)
    }

    private val getLearningpath =
      (apiOperation[LearningPathV2]("getLearningpath")
        summary "Fetch details about the specified learningpath"
        notes "Shows all information about the specified learningpath."
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        asQueryParam[Option[String]](language))
        responseMessages (response403, response404, response500)
        authorizations "oauth2")

    get("/:learningpath_id", operation(getLearningpath)) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val id = long(this.learningpathId.paramName)
      val userInfo = UserInfo.get

      readService.withIdV2(id, language, userInfo) match {
        case Some(x) => x
        case None =>
          halt(status = 404,
               body = Error(Error.NOT_FOUND, s"Learningpath with id $id and language $language not found"))
      }
    }

    private val getLearningpathStatus =
      (apiOperation[LearningPathStatus]("getLearningpathStatus")
        summary "Show status information for the learningpath"
        notes "Shows publishingstatus for the learningpath"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId))
        responseMessages (response403, response404, response500)
        authorizations "oauth2")

    get("/:learningpath_id/status/", operation(getLearningpathStatus)) {
      val id = long(this.learningpathId.paramName)
      readService.statusFor(id, UserInfo.get) match {
        case Some(x) => x
        case None =>
          halt(status = 404,
               body = Error(Error.NOT_FOUND, s"Learningpath with id $id and language $language not found"))
      }
    }

    private val getLearningsteps =
      (apiOperation[List[LearningStepSummaryV2]]("getLearningsteps")
        summary "Fetch learningsteps for given learningpath"
        notes "Show all learningsteps for given learningpath id"
        parameters (
          asHeaderParam[Option[String]](correlationId),
          asPathParam[String](learningpathId),
          asQueryParam[Option[String]](language)
      )
        responseMessages (response403, response404, response500)
        authorizations "oauth2")

    get("/:learningpath_id/learningsteps/", operation(getLearningsteps)) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val id = long(this.learningpathId.paramName)

      readService.learningstepsForWithStatusV2(id, StepStatus.ACTIVE, language, UserInfo.get) match {
        case Some(x) => x
        case None =>
          halt(status = 404,
               body = Error(Error.NOT_FOUND, s"Learningpath with id $id and language $language not found"))
      }
    }

    private val getLearningstep =
      (apiOperation[LearningStepV2]("getLearningstep")
        summary "Fetch details about the specified learningstep"
        notes "Show the given learningstep for the given learningpath"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        asPathParam[String](learningstepId),
        asQueryParam[Option[String]](language))
        responseMessages (response403, response404, response500, response502)
        authorizations "oauth2")

    get("/:learningpath_id/learningsteps/:learningstep_id", operation(getLearningstep)) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)

      readService.learningstepV2For(pathId, stepId, language, UserInfo.get) match {
        case Some(x) => x
        case None =>
          halt(status = 404,
               body = Error(
                 Error.NOT_FOUND,
                 s"Learningstep with id $stepId not found for learningpath with id $pathId and language $language"))
      }
    }

    private val getLearningStepsInTrash =
      (apiOperation[List[LearningStepSummaryV2]]("getLearningStepsInTrash")
        summary "Fetch deleted learningsteps for given learningpath"
        notes "Show all learningsteps for the given learningpath that are marked as deleted"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        asQueryParam[Option[String]](language))
        responseMessages (response403, response404, response500)
        authorizations "oauth2")

    get("/:learningpath_id/learningsteps/trash/", operation(getLearningStepsInTrash)) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val id = long(this.learningpathId.paramName)
      val userInfo = UserInfo(requireUserId)

      readService.learningstepsForWithStatusV2(id, StepStatus.DELETED, language, userInfo) match {
        case Some(x) => x
        case None =>
          halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id $id not found"))
      }
    }

    private val getLearningStepStatus =
      (apiOperation[LearningStepStatus]("getLearningStepStatus")
        summary "Show status information for learningstep"
        notes "Shows status for the learningstep"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        asPathParam[String](learningstepId))
        responseMessages (response403, response404, response500)
        authorizations "oauth2")

    get("/:learningpath_id/learningsteps/:learningstep_id/status/", operation(getLearningStepStatus)) {
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)

      readService.learningStepStatusForV2(pathId, stepId, Language.DefaultLanguage, UserInfo.get) match {
        case Some(x) => x
        case None =>
          halt(
            status = 404,
            body = Error(Error.NOT_FOUND, s"Learningstep with id $stepId not found for learningpath with id $pathId"))
      }
    }

    private val getMyLearningpaths =
      (apiOperation[List[LearningPathSummaryV2]]("getMyLearningpaths")
        summary "Fetch all learningspaths you have created"
        notes "Shows your learningpaths."
        parameters asHeaderParam[Option[String]](correlationId)
        responseMessages (response403, response500)
        authorizations "oauth2")

    get("/mine/", operation(getMyLearningpaths)) {
      readService.withOwnerV2(owner = requireUserId)
    }

    private val getLicenses =
      (apiOperation[List[License]]("getLicenses")
        summary "Show all valid licenses"
        notes "Shows all valid licenses"
        parameters (asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](licenseFilter))
        responseMessages (response403, response500)
        authorizations "oauth2")

    get("/licenses/", operation(getLicenses)) {
      val licenses: Seq[LicenseDefinition] =
        paramOrNone(this.licenseFilter.paramName) match {
          case None => mapping.License.getLicenses
          case Some(filter) =>
            mapping.License.getLicenses.filter(_.license.contains(filter))
        }

      licenses.map(x => License(x.license, Option(x.description), x.url))
    }

    private val addNewLearningpath =
      (apiOperation[LearningPathV2]("addLearningpath")
        summary "Store new learningpath"
        notes "Adds the given learningpath"
        parameters (asHeaderParam[Option[String]](correlationId),
        bodyParam[NewLearningPathV2])
        responseMessages (response400, response403, response404, response500)
        authorizations "oauth2")

    post("/", operation(addNewLearningpath)) {
      val newLearningPath = extract[NewLearningPathV2](request.body)
      val userInfo = UserInfo(requireUserId)
      updateService.addLearningPathV2(newLearningPath, userInfo) match {
        case None =>
          halt(status = 404, body = Error(Error.GENERIC, s"The chosen language is not supported"))
        case Some(learningPath) =>
          logger.info(s"CREATED LearningPath with ID =  ${learningPath.id}")
          halt(status = 201, headers = Map("Location" -> learningPath.metaUrl), body = learningPath)
      }
    }

    private val copyLearningpath =
      (apiOperation[LearningPathV2]("copyLearningpath")
        summary "Copy given learningpath and store it as a new learningpath"
        notes "Copies the given learningpath, with the option to override some fields"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        bodyParam[NewCopyLearningPathV2])
        responseMessages (response400, response403, response404, response500)
        authorizations "oauth2")

    post("/:learningpath_id/copy/", operation(copyLearningpath)) {
      val newLearningPath = extract[NewCopyLearningPathV2](request.body)
      val pathId = long(this.learningpathId.paramName)
      val userInfo = UserInfo(requireUserId)
      updateService.newFromExistingV2(pathId, newLearningPath, userInfo) match {
        case None =>
          halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id $pathId not found"))
        case Some(learningPath) =>
          logger.info(s"COPIED LearningPath with ID =  ${learningPath.id}")
          halt(status = 201, headers = Map("Location" -> learningPath.metaUrl), body = learningPath)
      }
    }

    private val updateLearningPath =
      (apiOperation[LearningPathV2]("updateLearningPath")
        summary "Update given learningpath"
        notes "Updates the given learningPath"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        bodyParam[UpdatedLearningPathV2])
        responseMessages (response400, response403, response404, response500)
        authorizations "oauth2")

    patch("/:learningpath_id", operation(updateLearningPath)) {
      val pathId = long(this.learningpathId.paramName)
      val userInfo = UserInfo(requireUserId)
      val updatedLearningPath =
        updateService.updateLearningPathV2(pathId, extract[UpdatedLearningPathV2](request.body), userInfo)
      updatedLearningPath match {
        case None =>
          halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id $pathId not found"))
        case Some(learningPath) =>
          logger.info(s"UPDATED LearningPath with ID =  ${learningPath.id}")
          Ok(body = learningPath)
      }
    }

    private val addNewLearningStep =
      (apiOperation[LearningStepV2]("addLearningStep")
        summary "Add new learningstep to learningpath"
        notes "Adds the given LearningStep"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        bodyParam[NewLearningStepV2])
        responseMessages (response400, response403, response404, response500, response502)
        authorizations "oauth2")

    post("/:learningpath_id/learningsteps/", operation(addNewLearningStep)) {
      val newLearningStep = extract[NewLearningStepV2](request.body)
      val pathId = long(this.learningpathId.paramName)
      val userInfo = UserInfo(requireUserId)
      val createdLearningStep =
        updateService.addLearningStepV2(pathId, newLearningStep, userInfo)
      createdLearningStep match {
        case None =>
          halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id $pathId not found"))
        case Some(learningStep) =>
          logger.info(s"CREATED LearningStep with ID =  ${learningStep.id} for LearningPath with ID = $pathId")
          halt(status = 201, headers = Map("Location" -> learningStep.metaUrl), body = createdLearningStep)
      }
    }

    private val updateLearningStep =
      (apiOperation[LearningStepV2]("updateLearningStep")
        summary "Update given learningstep"
        notes "Update the given learningStep"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        asPathParam[String](learningstepId),
        bodyParam[UpdatedLearningStepV2])
        responseMessages (response400, response403, response404, response500, response502)
        authorizations "oauth2")

    patch("/:learningpath_id/learningsteps/:learningstep_id", operation(updateLearningStep)) {
      val updatedLearningStep = extract[UpdatedLearningStepV2](request.body)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)
      val userInfo = UserInfo(requireUserId)
      val createdLearningStep =
        updateService.updateLearningStepV2(pathId, stepId, updatedLearningStep, userInfo)

      createdLearningStep match {
        case None =>
          halt(
            status = 404,
            body = Error(Error.NOT_FOUND, s"Learningstep with id $stepId for learningpath with id $pathId not found"))
        case Some(learningStep) =>
          logger.info(s"UPDATED LearningStep with ID = $stepId for LearningPath with ID = $pathId")
          Ok(body = learningStep)
      }
    }

    private val updateLearningstepSeqNo =
      (apiOperation[LearningStepSeqNo]("updatetLearningstepSeqNo")
        summary "Store new sequence number for learningstep."
        notes "Updates the sequence number for the given learningstep. The sequence number of other learningsteps will be affected by this."
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        asPathParam[String](learningstepId),
        bodyParam[LearningStepSeqNo])
        responseMessages (response403, response404, response500)
        authorizations "oauth2")

    put("/:learningpath_id/learningsteps/:learningstep_id/seqNo/", operation(updateLearningstepSeqNo)) {
      val newSeqNo = extract[LearningStepSeqNo](request.body)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)
      val userInfo = UserInfo(requireUserId)

      updateService.updateSeqNo(pathId, stepId, newSeqNo.seqNo, userInfo) match {
        case Some(seqNo) => seqNo
        case None =>
          halt(
            status = 404,
            body = Error(Error.NOT_FOUND, s"Learningstep with id $stepId not found for learningpath with id $pathId"))
      }
    }

    private val updateLearningStepStatus =
      (apiOperation[LearningStepV2]("updateLearningStepStatus")
        summary "Update status of given learningstep"
        notes "Updates the status of the given learningstep"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        asPathParam[String](learningstepId),
        bodyParam[LearningStepStatus])
        responseMessages (response400, response403, response404, response500)
        authorizations "oauth2")

    put("/:learningpath_id/learningsteps/:learningstep_id/status/", operation(updateLearningStepStatus)) {
      val learningStepStatus = extract[LearningStepStatus](request.body)
      val stepStatus = StepStatus.valueOfOrError(learningStepStatus.status)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)
      val userInfo = UserInfo(requireUserId)

      val updatedStep = updateService.updateLearningStepStatusV2(pathId, stepId, stepStatus, userInfo)
      updatedStep match {
        case None =>
          halt(
            status = 404,
            body = Error(Error.NOT_FOUND, s"Learningstep with id $stepId for learningpath with id $pathId not found"))
        case Some(learningStep) =>
          logger.info(
            s"UPDATED LearningStep with id: $stepId for LearningPath with id: $pathId to STATUS = ${learningStep.status}")
          Ok(body = learningStep)
      }
    }

    private val updateLearningPathStatus =
      (apiOperation[LearningPathStatus]("updateLearningPathStatus")
        summary "Update status of given learningpath"
        notes "Updates the status of the given learningPath"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        bodyParam[LearningPathStatus],
        asQueryParam[Option[String]](adminMessage))
        responseMessages (response400, response403, response404, response500)
        authorizations "oauth2")

    put("/:learningpath_id/status/", operation(updateLearningPathStatus)) {
      val learningPathStatus = extract[LearningPathStatus](request.body)
      val pathStatus =
        domain.LearningPathStatus.valueOfOrError(learningPathStatus.status)
      val pathId = long(this.learningpathId.paramName)
      val userInfo = UserInfo(requireUserId)
      val message = paramOrNone(this.adminMessage.paramName)

      updateService.updateLearningPathStatusV2(pathId, pathStatus, userInfo, Language.DefaultLanguage, message) match {
        case None =>
          halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id $pathId not found"))
        case Some(learningPath) =>
          logger.info(s"UPDATED status of LearningPath with ID = ${learningPath.id}")
          Ok(body = learningPath)
      }
    }

    private val withStatus: SwaggerSupportSyntax.OperationBuilder =
      (apiOperation[List[LearningPathV2]]("withStatus")
        summary "Fetch all learningpaths with specified status"
        notes "Fetch all learningpaths with specified status"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningPathStatus))
        responseMessages (response500, response400)
        authorizations "oauth2")
    get(s"/status/:${this.learningPathStatus.paramName}", operation(withStatus)) {
      val pathStatus = params(this.learningPathStatus.paramName)
      readService.learningPathWithStatus(pathStatus, UserInfo.get) match {
        case Success(lps) => lps
        case Failure(ex)  => errorHandler(ex)
      }
    }

    private val deleteLearningPath =
      (apiOperation[LearningPathV2]("deleteLearningPath")
        summary "Delete given learningpath"
        notes "Deletes the given learningPath"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId))
        responseMessages (response403, response404, response500)
        authorizations "oauth2")

    delete("/:learningpath_id", operation(deleteLearningPath)) {
      val pathId = long(this.learningpathId.paramName)
      val userInfo = UserInfo(requireUserId)
      val deleted =
        updateService.updateLearningPathStatusV2(pathId,
                                                 domain.LearningPathStatus.DELETED,
                                                 userInfo,
                                                 Language.DefaultLanguage)
      deleted match {
        case None =>
          halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id $pathId not found"))
        case Some(_) =>
          logger.info(s"MARKED LearningPath with ID: $pathId as DELETED")
          halt(status = 204)
      }
    }

    private val deleteLearningStep =
      (apiOperation[Void]("deleteLearningStep")
        summary "Delete given learningstep"
        notes "Deletes the given learningStep"
        parameters (asHeaderParam[Option[String]](correlationId),
        asPathParam[String](learningpathId),
        asPathParam[String](learningstepId))
        responseMessages (response403, response404, response500)
        authorizations "oauth2")

    delete("/:learningpath_id/learningsteps/:learningstep_id", operation(deleteLearningStep)) {
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)
      val userInfo = UserInfo(requireUserId)
      val deleted = updateService.updateLearningStepStatusV2(pathId, stepId, StepStatus.DELETED, userInfo)
      deleted match {
        case None =>
          halt(
            status = 404,
            body = Error(Error.NOT_FOUND, s"Learningstep with id $stepId for learningpath with id $pathId not found"))
        case Some(_) =>
          logger.info(s"MARKED LearningStep with id: $stepId for LearningPath with id: $pathId as DELETED.")
          halt(status = 204)
      }
    }

    private val getTags =
      (apiOperation[List[LearningPathTags]]("getTags")
        summary "Fetch all previously used tags in learningpaths"
        notes "Retrieves a list of all previously used tags in learningpaths"
        parameters (asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](language))
        responseMessages response500
        authorizations "oauth2")

    get("/tags/", operation(getTags)) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val allTags = readService.tags

      converterService.asApiLearningPathTagsSummary(allTags, language) match {
        case Some(s) => s
        case None =>
          halt(status = 404, body = Error(Error.NOT_FOUND, s"Tags with language '$language' not found"))
      }
    }

    private val getContributors =
      (apiOperation[List[Author]]("getContributors")
        summary "Fetch all previously used contributors in learningpaths"
        notes "Retrieves a list of all previously used contributors in learningpaths"
        parameters asHeaderParam[Option[String]](correlationId)
        responseMessages response500
        authorizations "oauth2")

    get("/contributors/", operation(getContributors)) {
      readService.contributors
    }
  }
}
