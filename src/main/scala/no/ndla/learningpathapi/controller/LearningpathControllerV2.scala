/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.{
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  InitialScrollContextKeywords
}
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.{
  Author => _,
  LearningPathStatus => _,
  LearningPathTags => _,
  License => _,
  _
}
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchService}
import no.ndla.learningpathapi.service.{ConverterService, ReadService, UpdateService}
import no.ndla.learningpathapi.validation.LanguageValidator
import no.ndla.mapping
import no.ndla.mapping.LicenseDefinition
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger._
import org.scalatra.util.NotNothing
import org.scalatra.{Created, NoContent, NotFound, Ok, ScalatraServlet}

import scala.util.{Failure, Success, Try}

trait LearningpathControllerV2 {

  this: ReadService
    with UpdateService
    with SearchService
    with LanguageValidator
    with ConverterService
    with SearchConverterServiceComponent =>
  val learningpathControllerV2: LearningpathControllerV2

  class LearningpathControllerV2(implicit val swagger: Swagger)
      extends NdlaController
      with ScalatraServlet
      with NativeJsonSupport
      with SwaggerSupport
      with LazyLogging
      with CorrelationIdSupport {
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

    case class Param[T](paramName: String, description: String)

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only Learningpaths with content matching the specified query.")
    private val language =
      Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.values.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val pageNo =
      Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize =
      Param[Option[Int]]("page-size", "The number of search hits to display for each page.")
    private val learningpathId =
      Param[String]("learningpath_id", "Id of the learningpath.")
    private val learningstepId =
      Param[String]("learningstep_id", "Id of the learningstep.")
    private val tag = Param[Option[String]]("tag", "Return only Learningpaths that are tagged with this exact tag.")
    private val learningpathIds = Param[Option[String]](
      "ids",
      "Return only Learningpaths that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val licenseFilter =
      Param[Option[String]]("filter",
                            "Query for filtering licenses. Only licenses containing filter-string are returned.")
    private val fallback = Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
    private val createResourceIfMissing =
      Param[Option[Boolean]]("create-if-missing", "Create taxonomy resource if missing for learningPath")
    private val learningPathStatus =
      Param[String]("STATUS", "Status of LearningPaths")
    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
           .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}' and '${this.fallback.paramName}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
         |""".stripMargin
    )
    private val verificationStatus =
      Param[Option[String]]("verificationStatus", "Return only learning paths that have this verification status.")

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)
    private def asFormParam[T: Manifest: NotNothing](param: Param[T]) =
      formParam[T](param.paramName).description(param.description)
    private def asFileParam(param: Param[_]) =
      Parameter(name = param.paramName,
                `type` = ValueDataType("file"),
                description = Some(param.description),
                paramType = ParamType.Form)

    /**
      * Does a scroll with [[SearchService]]
      * If no scrollId is specified execute the function @orFunction in the second parameter list.
      *
      * @param orFunction Function to execute if no scrollId in parameters (Usually searching)
      * @return A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: String)(orFunction: => Any): Any = {
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          searchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId
                .map(i => this.scrollId.paramName -> i)
                .toMap
              Ok(searchConverterService.asApiSearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case _ => orFunction
      }
    }

    private def search(query: Option[String],
                       searchLanguage: String,
                       tag: Option[String],
                       idList: List[Long],
                       sort: Option[String],
                       pageSize: Option[Int],
                       page: Option[Int],
                       fallback: Boolean,
                       verificationStatus: Option[String],
                       shouldScroll: Boolean) = {
      val settings = query match {
        case Some(q) =>
          SearchSettings(
            query = Some(q),
            withIdIn = idList,
            taggedWith = tag,
            withPaths = List.empty,
            searchLanguage = Language.getLanguageOrDefaultIfUnsupported(searchLanguage),
            sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
            page = page,
            pageSize = pageSize,
            fallback = fallback,
            verificationStatus = verificationStatus,
            shouldScroll = shouldScroll,
            status = List(domain.LearningPathStatus.PUBLISHED)
          )
        case None =>
          SearchSettings(
            query = None,
            withIdIn = idList,
            taggedWith = tag,
            withPaths = List.empty,
            searchLanguage = Language.getLanguageOrDefaultIfUnsupported(searchLanguage),
            sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc),
            page = page,
            pageSize = pageSize,
            fallback = fallback,
            verificationStatus = verificationStatus,
            shouldScroll = shouldScroll,
            status = List(domain.LearningPathStatus.PUBLISHED)
          )
      }

      searchService.matchingQuery(settings) match {
        case Success(searchResult) =>
          val responseHeader =
            searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiSearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/",
      operation(
        apiOperation[SearchResultV2]("getLearningpaths")
          .summary("Find public learningpaths")
          .description("Show public learningpaths.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(tag),
            asQueryParam(learningpathIds),
            asQueryParam(language),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(sort),
            asQueryParam(fallback),
            asQueryParam(scrollId),
            asQueryParam(verificationStatus)
          )
          .responseMessages(response400, response500)
          .authorizations("oauth2")
      )
    ) {
      val scrollId = paramOrNone(this.scrollId.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

      scrollSearchOr(scrollId, language) {
        val query = paramOrNone(this.query.paramName)
        val tag = paramOrNone(this.tag.paramName)
        val idList = paramAsListOfLong(this.learningpathIds.paramName)
        val sort = paramOrNone(this.sort.paramName)
        val pageSize = paramOrNone(this.pageSize.paramName).flatMap(ps => Try(ps.toInt).toOption)
        val page = paramOrNone(this.pageNo.paramName).flatMap(idx => Try(idx.toInt).toOption)
        val fallback =
          booleanOrDefault(this.fallback.paramName, default = false)
        val verificationStatus = paramOrNone(this.verificationStatus.paramName)
        val shouldScroll = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)

        search(query, language, tag, idList, sort, pageSize, page, fallback, verificationStatus, shouldScroll)
      }
    }

    post(
      "/search/",
      operation(
        apiOperation[List[SearchResultV2]]("searchArticles")
          .summary("Find public learningpaths")
          .description("Show public learningpaths")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[SearchParams],
            asQueryParam(scrollId)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response500))
    ) {
      val searchParams = extract[SearchParams](request.body)
      val language = searchParams.language.getOrElse(Language.AllLanguages)

      scrollSearchOr(searchParams.scrollId, language) {
        val query = searchParams.query
        val tag = searchParams.tag
        val idList = searchParams.ids
        val sort = searchParams.sort
        val pageSize = searchParams.pageSize
        val page = searchParams.page
        val fallback = searchParams.fallback.getOrElse(false)
        val verificationStatus = searchParams.verificationStatus
        val shouldScroll = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)

        search(query, language, tag, idList, sort, pageSize, page, fallback, verificationStatus, shouldScroll)
      }
    }

    get(
      "/:learningpath_id",
      operation(
        apiOperation[LearningPathV2]("getLearningpath")
          .summary("Fetch details about the specified learningpath")
          .description("Shows all information about the specified learningpath.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val id = long(this.learningpathId.paramName)
      val userInfo = UserInfo.getUserOrPublic
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.withIdV2(id, language, fallback, userInfo) match {
        case Success(lp) => Ok(lp)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/:learningpath_id/status/",
      operation(
        apiOperation[LearningPathStatus]("getLearningpathStatus")
          .summary("Show status information for the learningpath")
          .description("Shows publishingstatus for the learningpath")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId)
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val id = long(this.learningpathId.paramName)
      readService.statusFor(id, UserInfo.getUserOrPublic) match {
        case Success(status) => Ok(status)
        case Failure(ex)     => errorHandler(ex)
      }
    }

    get(
      "/:learningpath_id/learningsteps/",
      operation(
        apiOperation[List[LearningStepSummaryV2]]("getLearningsteps")
          .summary("Fetch learningsteps for given learningpath")
          .description("Show all learningsteps for given learningpath id")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val id = long(this.learningpathId.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.learningstepsForWithStatusV2(id, StepStatus.ACTIVE, language, fallback, UserInfo.getUserOrPublic) match {
        case Success(x)  => Ok(x)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/:learningpath_id/learningsteps/:learningstep_id",
      operation(
        apiOperation[LearningStepV2]("getLearningstep")
          .summary("Fetch details about the specified learningstep")
          .description("Show the given learningstep for the given learningpath")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asPathParam(learningstepId),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .responseMessages(response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.learningstepV2For(pathId, stepId, language, fallback, UserInfo.getUserOrPublic) match {
        case Success(step) => Ok(step)
        case Failure(ex)   => errorHandler(ex)
      }
    }

    get(
      "/:learningpath_id/learningsteps/trash/",
      operation(
        apiOperation[List[LearningStepSummaryV2]]("getLearningStepsInTrash")
          .summary("Fetch deleted learningsteps for given learningpath")
          .description("Show all learningsteps for the given learningpath that are marked as deleted")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val id = long(this.learningpathId.paramName)
      val userInfo = UserInfo(requireUserId)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.learningstepsForWithStatusV2(id, StepStatus.DELETED, language, fallback, userInfo) match {
        case Success(x)  => Ok(x)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/:learningpath_id/learningsteps/:learningstep_id/status/",
      operation(
        apiOperation[LearningStepStatus]("getLearningStepStatus")
          .summary("Show status information for learningstep")
          .description("Shows status for the learningstep")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asPathParam(learningstepId),
            asQueryParam(fallback)
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.learningStepStatusForV2(pathId, stepId, Language.DefaultLanguage, fallback, UserInfo.getUserOrPublic) match {
        case Success(status) => Ok(status)
        case Failure(ex)     => errorHandler(ex)
      }
    }

    get(
      "/mine/",
      operation(
        apiOperation[List[LearningPathSummaryV2]]("getMyLearningpaths")
          .summary("Fetch all learningspaths you have created")
          .description("Shows your learningpaths.")
          .parameters(asHeaderParam(correlationId))
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      readService.withOwnerV2(UserInfo(requireUserId))
    }

    get(
      "/licenses/",
      operation(
        apiOperation[List[License]]("getLicenses")
          .summary("Show all valid licenses")
          .description("Shows all valid licenses")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(licenseFilter)
          )
          .responseMessages(response403, response500)
          .authorizations("oauth2")
      )
    ) {
      val licenses: Seq[LicenseDefinition] =
        paramOrNone(this.licenseFilter.paramName) match {
          case None => mapping.License.getLicenses
          case Some(filter) =>
            mapping.License.getLicenses
              .filter(_.license.toString.contains(filter))
        }

      licenses.map(x => License(x.license.toString, Option(x.description), x.url))
    }

    post(
      "/",
      operation(
        apiOperation[LearningPathV2]("addLearningpath")
          .summary("Store new learningpath")
          .description("Adds the given learningpath")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[NewLearningPathV2]
          )
          .responseMessages(response400, response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val newLearningPath = extract[NewLearningPathV2](request.body)
      val userInfo = UserInfo(requireUserId)
      updateService.addLearningPathV2(newLearningPath, userInfo) match {
        case Failure(ex) => errorHandler(ex)
        case Success(learningPath) =>
          logger.info(s"CREATED LearningPath with ID =  ${learningPath.id}")
          Created(headers = Map("Location" -> learningPath.metaUrl), body = learningPath)
      }
    }

    post(
      "/:learningpath_id/copy/",
      operation(
        apiOperation[LearningPathV2]("copyLearningpath")
          .summary("Copy given learningpath and store it as a new learningpath")
          .description("Copies the given learningpath, with the option to override some fields")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            bodyParam[NewCopyLearningPathV2]
          )
          .responseMessages(response400, response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      UserInfo.getWithUserIdOrAdmin match {
        case Failure(ex) => errorHandler(ex)
        case Success(userInfo) =>
          val newLearningPath = extract[NewCopyLearningPathV2](request.body)
          val pathId = long(this.learningpathId.paramName)
          updateService.newFromExistingV2(pathId, newLearningPath, userInfo) match {
            case Success(learningPath) =>
              logger.info(s"COPIED LearningPath with ID =  ${learningPath.id}")
              Created(headers = Map("Location" -> learningPath.metaUrl), body = learningPath)
            case Failure(ex) => errorHandler(ex)
          }
      }
    }

    patch(
      "/:learningpath_id",
      operation(
        apiOperation[LearningPathV2]("updateLearningPath")
          .summary("Update given learningpath")
          .description("Updates the given learningPath")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            bodyParam[UpdatedLearningPathV2]
          )
          .responseMessages(response400, response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val userInfo = UserInfo(requireUserId)
      val pathId = long(this.learningpathId.paramName)
      updateService.updateLearningPathV2(pathId, extract[UpdatedLearningPathV2](request.body), userInfo) match {
        case Success(lp) => Ok(lp)
        case Failure(ex) => errorHandler(ex)
      }
    }

    post(
      "/:learningpath_id/learningsteps/",
      operation(
        apiOperation[LearningStepV2]("addLearningStep")
          .summary("Add new learningstep to learningpath")
          .description("Adds the given LearningStep")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            bodyParam[NewLearningStepV2]
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val userInfo = UserInfo(requireUserId)
      val newLearningStep = extract[NewLearningStepV2](request.body)
      val pathId = long(this.learningpathId.paramName)
      updateService.addLearningStepV2(pathId, newLearningStep, userInfo) match {
        case Failure(ex) => errorHandler(ex)
        case Success(learningStep) =>
          logger.info(s"CREATED LearningStep with ID =  ${learningStep.id} for LearningPath with ID = $pathId")
          Created(headers = Map("Location" -> learningStep.metaUrl), body = learningStep)
      }
    }

    patch(
      "/:learningpath_id/learningsteps/:learningstep_id",
      operation(
        apiOperation[LearningStepV2]("updateLearningStep")
          .summary("Update given learningstep")
          .description("Update the given learningStep")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asPathParam(learningstepId),
            bodyParam[UpdatedLearningStepV2]
          )
          .responseMessages(response400, response403, response404, response500, response502)
          .authorizations("oauth2")
      )
    ) {
      val userInfo = UserInfo(requireUserId)
      val updatedLearningStep = extract[UpdatedLearningStepV2](request.body)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)
      val createdLearningStep =
        updateService.updateLearningStepV2(pathId, stepId, updatedLearningStep, userInfo)

      createdLearningStep match {
        case Failure(ex) => errorHandler(ex)
        case Success(learningStep) =>
          logger.info(s"UPDATED LearningStep with ID = $stepId for LearningPath with ID = $pathId")
          Ok(learningStep)
      }
    }

    put(
      "/:learningpath_id/learningsteps/:learningstep_id/seqNo/",
      operation(
        apiOperation[LearningStepSeqNo]("updatetLearningstepSeqNo")
          .summary("Store new sequence number for learningstep.")
          .description(
            "Updates the sequence number for the given learningstep. The sequence number of other learningsteps will be affected by this.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asPathParam(learningstepId),
            bodyParam[LearningStepSeqNo]
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val userInfo = UserInfo(requireUserId)
      val newSeqNo = extract[LearningStepSeqNo](request.body)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)

      updateService.updateSeqNo(pathId, stepId, newSeqNo.seqNo, userInfo) match {
        case Success(seqNo) => Ok(seqNo)
        case Failure(ex)    => errorHandler(ex)
      }
    }

    put(
      "/:learningpath_id/learningsteps/:learningstep_id/status/",
      operation(
        apiOperation[LearningStepV2]("updateLearningStepStatus")
          .summary("Update status of given learningstep")
          .description("Updates the status of the given learningstep")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asPathParam(learningstepId),
            bodyParam[LearningStepStatus]
          )
          .responseMessages(response400, response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val userInfo = UserInfo(requireUserId)
      val learningStepStatus = extract[LearningStepStatus](request.body)
      val stepStatus = StepStatus.valueOfOrError(learningStepStatus.status)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)

      val updatedStep = updateService.updateLearningStepStatusV2(pathId, stepId, stepStatus, userInfo)

      updatedStep match {
        case Failure(ex) => errorHandler(ex)
        case Success(learningStep) =>
          logger.info(
            s"UPDATED LearningStep with id: $stepId for LearningPath with id: $pathId to STATUS = ${learningStep.status}")
          Ok(learningStep)
      }
    }

    put(
      "/:learningpath_id/status/",
      operation(
        apiOperation[LearningPathV2]("updateLearningPathStatus")
          .summary("Update status of given learningpath")
          .description("Updates the status of the given learningPath")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            bodyParam[UpdateLearningPathStatus]
          )
          .responseMessages(response400, response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val userInfo = UserInfo(requireUserId)
      val toUpdate = extract[UpdateLearningPathStatus](request.body)
      val pathStatus = domain.LearningPathStatus.valueOfOrError(toUpdate.status)
      val pathId = long(this.learningpathId.paramName)

      updateService.updateLearningPathStatusV2(pathId, pathStatus, userInfo, Language.DefaultLanguage, toUpdate.message) match {
        case Failure(ex) => errorHandler(ex)
        case Success(learningPath) =>
          logger.info(s"UPDATED status of LearningPath with ID = ${learningPath.id}")
          Ok(learningPath)
      }
    }

    get(
      s"/status/:${this.learningPathStatus.paramName}",
      operation(
        apiOperation[List[LearningPathV2]]("withStatus")
          .summary("Fetch all learningpaths with specified status")
          .description("Fetch all learningpaths with specified status")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningPathStatus)
          )
          .responseMessages(response500, response400)
          .authorizations("oauth2")
      )
    ) {
      val pathStatus = params(this.learningPathStatus.paramName)
      readService.learningPathWithStatus(pathStatus, UserInfo.getUserOrPublic) match {
        case Success(lps) => Ok(lps)
        case Failure(ex)  => errorHandler(ex)
      }
    }

    delete(
      "/:learningpath_id",
      operation(
        apiOperation[LearningPathV2]("deleteLearningPath")
          .summary("Delete given learningpath")
          .description("Deletes the given learningPath")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId)
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2"))
    ) {
      val userInfo = UserInfo(requireUserId)
      val pathId = long(this.learningpathId.paramName)

      updateService.updateLearningPathStatusV2(
        pathId,
        domain.LearningPathStatus.DELETED,
        userInfo,
        Language.DefaultLanguage
      ) match {
        case Failure(ex) => errorHandler(ex)
        case Success(_) =>
          logger.info(s"MARKED LearningPath with ID: $pathId as DELETED")
          NoContent()
      }
    }

    delete(
      "/:learningpath_id/learningsteps/:learningstep_id",
      operation(
        apiOperation[Void]("deleteLearningStep")
          .summary("Delete given learningstep")
          .description("Deletes the given learningStep")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asPathParam(learningstepId)
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val userInfo = UserInfo(requireUserId)
      val pathId = long(this.learningpathId.paramName)
      val stepId = long(this.learningstepId.paramName)
      val deleted = updateService.updateLearningStepStatusV2(pathId, stepId, StepStatus.DELETED, userInfo)
      deleted match {
        case Failure(ex) => errorHandler(ex)
        case Success(_) =>
          logger.info(s"MARKED LearningStep with id: $stepId for LearningPath with id: $pathId as DELETED.")
          NoContent()
      }
    }

    get(
      "/tags/",
      operation(
        apiOperation[List[LearningPathTags]]("getTags")
          .summary("Fetch all previously used tags in learningpaths")
          .description("Retrieves a list of all previously used tags in learningpaths")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .responseMessages(response500)
          .authorizations("oauth2")
      )
    ) {
      val language =
        paramOrDefault(this.language.paramName, Language.AllLanguages)
      val allTags = readService.tags
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      converterService.asApiLearningPathTagsSummary(allTags, language, fallback) match {
        case Some(s) => s
        case None =>
          NotFound(Error(Error.NOT_FOUND, s"Tags with language '$language' not found"))
      }
    }

    get(
      "/contributors/",
      operation(
        apiOperation[List[Author]]("getContributors")
          .summary("Fetch all previously used contributors in learningpaths")
          .description("Retrieves a list of all previously used contributors in learningpaths")
          .parameters(asHeaderParam(correlationId))
          .responseMessages(response500)
          .authorizations("oauth2")
      )
    ) {
      readService.contributors
    }

    post(
      "/:learningpath_id/update-taxonomy/",
      operation(
        apiOperation[LearningPathV2]("updateLearningPathTaxonomy")
          .summary("Update taxonomy for specified learningpath")
          .description("Update taxonomy for specified learningpath")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(learningpathId),
            asQueryParam(language),
            asQueryParam(fallback),
            asQueryParam(createResourceIfMissing)
          )
          .responseMessages(response403, response404, response500)
          .authorizations("oauth2")
      )
    ) {
      val userInfo = UserInfo(requireUserId)
      val pathId = long(this.learningpathId.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val createResourceIfMissing = booleanOrDefault(this.createResourceIfMissing.paramName, default = false)

      updateService.updateTaxonomyForLearningPath(pathId, createResourceIfMissing, language, fallback, userInfo) match {
        case Success(lp) => Ok(lp)
        case Failure(ex) => errorHandler(ex)
      }
    }
  }
}
