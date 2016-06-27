package no.ndla.learningpathapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.UsernameHeader
import no.ndla.learningpathapi.model.api.{LearningPath, LearningPathStatus, LearningPathTags, LearningStep, _}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.service.search.SearchServiceComponent
import no.ndla.learningpathapi.service.{ReadServiceComponent, UpdateServiceComponent}
import no.ndla.learningpathapi.validation.LanguageValidator
import no.ndla.learningpathapi.{ComponentRegistry, LearningpathApiProperties}
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.{NativeJsonSupport}
import org.scalatra.swagger.{Swagger, SwaggerSupport}
import org.scalatra.{Ok, ScalatraServlet}

import scala.util.Try

trait LearningpathController {

  this: ReadServiceComponent with UpdateServiceComponent with SearchServiceComponent =>
  val learningpathController: LearningpathController

  class LearningpathController(implicit val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "API for accessing Learningpaths from ndla.no."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val response400 = ResponseMessageWithModel(400, "Validation Error", "ValidationError")
    val response403 = ResponseMessageWithModel(403, "Access not granted", "Error")
    val response404 = ResponseMessageWithModel(404, "Not found", "Error")
    val response500 = ResponseMessageWithModel(500, "Unknown error", "Error")
    val response502 = ResponseMessageWithModel(502, "Remote error", "Error")

    val getLearningpaths =
      (apiOperation[SearchResult]("getLearningpaths")
        summary "Show public learningpaths"
        notes "Show public learningpaths."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        queryParam[Option[String]]("query").description("Return only Learningpaths with content matching the specified query."),
        queryParam[Option[String]]("tag").description("Return only Learningpaths that are tagged with this exact tag."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
        queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
        queryParam[Option[Int]]("page-size").description(s"The number of search hits to display for each page. Default is ${LearningpathApiProperties.DefaultPageSize}. Max page-size is ${LearningpathApiProperties.MaxPageSize}"),
        queryParam[Option[String]]("sort").description(
          """The sorting used on results.
           Default is by -relevance (desc) when querying.
           When browsing, the default is title (asc).
           The following are supported: relevance, -relevance, lastUpdated, -lastUpdated, duration, -duration, title, -title""".stripMargin))
        responseMessages(response400, response500))

    val getMyLearningpaths =
      (apiOperation[List[LearningPathSummary]]("getMyLearningpaths")
        summary "Show your learningpaths"
        notes "Shows your learningpaths."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."))
        responseMessages(response403, response500))

    val getLearningpath =
      (apiOperation[LearningPath]("getLearningpath")
        summary "Show details about the specified learningpath"
        notes "Shows all information about the specified learningpath."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500))

    val getLearningpathStatus =
      (apiOperation[LearningPathStatus]("getLearningpathStatus")
        summary "Show publishingstatus for the learningpath"
        notes "Shows publishingstatus for the learningpath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500))

    val getLearningStepStatus =
      (apiOperation[LearningStepStatus]("getLearningStepStatus")
        summary "Show status for the learningstep"
        notes "Shows status for the learningstep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."))
        responseMessages(response403, response404, response500))

    val getLearningsteps =
      (apiOperation[List[LearningStepSummary]]("getLearningsteps")
        summary "Show all learningsteps for given learningpath id"
        notes "Show all learningsteps for given learningpath id"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500))

    val getLearningStepsInTrash =
      (apiOperation[List[LearningStepSummary]]("getLearningStepsInTrash")
        summary "Show all learningsteps for the given learningpath that are marked as deleted"
        notes "Show all learningsteps for the given learningpath that are marked as deleted"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500))

    val getLearningstep =
      (apiOperation[LearningStep]("getLearningstep")
        summary "Show the given learningstep for the given learningpath"
        notes "Show the given learningstep for the given learningpath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."))
        responseMessages(response403, response404, response500, response502))

    val addNewLearningpath =
      (apiOperation[LearningPath]("addLearningpath")
        summary "Adds the given learningpath"
        notes "Adds the given learningpath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        queryParam[Option[String]]("copy-from").description("Id of learningPath to use as basis for the new one."),
        bodyParam[NewLearningPath])
        responseMessages(response400, response403, response404, response500))


    val addNewLearningStep =
      (apiOperation[LearningStep]("addLearningStep")
        summary "Adds the given LearningStep"
        notes "Adds the given LearningStep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        bodyParam[NewLearningStep])
        responseMessages(response400, response403, response404, response500, response502))

    val updateLearningPath =
      (apiOperation[LearningPath]("updateLearningPath")
        summary "Update the given learningpath"
        notes "Updates the given learningPath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        bodyParam[UpdatedLearningPath])
        responseMessages(response400, response403, response404, response500))

    val updateLearningStep =
      (apiOperation[LearningStep]("updateLearningStep")
        summary "Updates the given learningStep"
        notes "Update the given learningStep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."),
        bodyParam[UpdatedLearningStep])
        responseMessages(response400, response403, response404, response500, response502))

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
        responseMessages(response403, response404, response500))

    val updateLearningPathStatus =
      (apiOperation[LearningPathStatus]("updateLearningPathStatus")
        summary "Updates the status of the given learningPath"
        notes "Updates the status of the given learningPath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        bodyParam[LearningPathStatus])
        responseMessages(response400, response403, response404, response500))

    val updateLearningStepStatus =
      (apiOperation[LearningStep]("updateLearningStepStatus")
        summary "Updates the status of the given learningstep"
        notes "Updates the status of the given learningstep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."),
        bodyParam[LearningStepStatus])
        responseMessages(response400, response403, response404, response500))

    val deleteLearningPath =
      (apiOperation[LearningPath]("deleteLearningPath")
        summary "Deletes the given learningPath"
        notes "Deletes the given learningPath"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."))
        responseMessages(response403, response404, response500))

    val deleteLearningStep =
      (apiOperation[Void]("deleteLearningStep")
        summary "Deletes the given learningStep"
        notes "Deletes the given learningStep"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        pathParam[String]("path_id").description("The id of the learningpath."),
        pathParam[String]("step_id").description("The id of the learningstep."))
        responseMessages(response403, response404, response500))

    val getTags =
      (apiOperation[List[LearningPathTags]]("getTags")
        summary "Retrieves a list of all previously used tags in learningpaths"
        notes "Retrieves a list of all previously used tags in learningpaths"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."))
        responseMessages response500)

    before() {
      contentType = formats("json")
      LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
      ApplicationUrl.set(request)
    }

    after() {
      LoggerContext.clearCorrelationID()
      ApplicationUrl.clear()
    }

    error {
      case v: ValidationException => halt(status = 400, body = ValidationError(messages = v.errors))
      case a: AccessDeniedException => halt(status = 403, body = Error(Error.ACCESS_DENIED, a.getMessage))
      case ole: OptimisticLockException => halt(status = 409, body = Error(Error.RESOURCE_OUTDATED, Error.RESOURCE_OUTDATED_DESCRIPTION))
      case hre: HttpRequestException => halt(status = 502, body = Error(Error.REMOTE_ERROR, hre.getMessage))
      case t: Throwable => {
        t.printStackTrace()
        logger.error(t.getMessage)
        halt(status = 500, body = Error())
      }
    }

    get("/", operation(getLearningpaths)) {
      val query = paramOrNone("query")
      val tag = paramOrNone("tag")
      val language = LanguageValidator.validate("language", paramOrNone("language"))
      val sort = paramOrNone("sort")
      val pageSize = paramOrNone("page-size").flatMap(ps => Try(ps.toInt).toOption)
      val page = paramOrNone("page").flatMap(idx => Try(idx.toInt).toOption)
      logger.info("GET / with params query='{}', language={}, tag={}, page={}, page-size={}", query, language, tag, page, pageSize)

      query match {
        case Some(q) => searchService.matchingQuery(
          query = q.toLowerCase.split(" ").map(_.trim),
          taggedWith = tag,
          language = language,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
          page = page,
          pageSize = pageSize
        )
        case None => searchService.all(
          taggedWith = tag,
          sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc),
          language = language,
          page = page,
          pageSize = pageSize)
      }
    }


    get("/:path_id/?", operation(getLearningpath)) {
      readService.withId(long("path_id"), optionalUsernameFromHeader) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      }
    }

    get("/:path_id/status/?", operation(getLearningpathStatus)) {
      readService.statusFor(long("path_id"), optionalUsernameFromHeader) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      }
    }

    get("/:path_id/learningsteps/?", operation(getLearningsteps)) {
      readService.learningstepsForWithStatus(long("path_id"), StepStatus.ACTIVE, optionalUsernameFromHeader) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      }
    }

    get("/:path_id/learningsteps/:step_id/?", operation(getLearningstep)) {
      readService.learningstepFor(long("path_id"), long("step_id"), optionalUsernameFromHeader) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
      }
    }

    get("/:path_id/learningsteps/trash/?", operation(getLearningStepsInTrash)) {
      readService.learningstepsForWithStatus(long("path_id"), StepStatus.DELETED, Some(usernameFromHeader)) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      }
    }

    get("/:path_id/learningsteps/:step_id/status/?", operation(getLearningStepStatus)) {
      readService.learningStepStatusFor(long("path_id"), long("step_id"), optionalUsernameFromHeader) match {
        case Some(x) => x
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
      }
    }

    get("/mine/?", operation(getMyLearningpaths)) {
      readService.withOwner(owner = usernameFromHeader)
    }

    post("/", operation(addNewLearningpath)) {
      val newLearningPath = extract[NewLearningPath](request.body)
      val createdLearningPath: Option[LearningPath] = optLong("copy-from") match {
        case Some(id) => updateService.newFromExisting(id, newLearningPath, usernameFromHeader)
        case None => Some(updateService.addLearningPath(newLearningPath, usernameFromHeader))
      }

      createdLearningPath match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("copy-from")} not found"))
        case Some(learningPath) => {
          logger.info(s"CREATED LearningPath with ID =  ${learningPath.id}")
          halt(status = 201, headers = Map("Location" -> learningPath.metaUrl), body = createdLearningPath)
        }
      }
    }

    patch("/:path_id/?", operation(updateLearningPath)) {
      val updatedLearningPath = updateService.updateLearningPath(long("path_id"), extract[UpdatedLearningPath](request.body), usernameFromHeader)
      updatedLearningPath match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
        case Some(learningPath) => {
          logger.info(s"UPDATED LearningPath with ID =  ${learningPath.id}")
          Ok(body = learningPath)
        }
      }
    }

    post("/:path_id/learningsteps/?", operation(addNewLearningStep)) {
      val newLearningStep = extract[NewLearningStep](request.body)
      val createdLearningStep = updateService.addLearningStep(long("path_id"), newLearningStep, usernameFromHeader)
      createdLearningStep match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
        case Some(learningStep) => {
          logger.info(s"CREATED LearningStep with ID =  ${learningStep.id} for LearningPath with ID = ${params("path_id")}")
          halt(status = 201, headers = Map("Location" -> learningStep.metaUrl), body = createdLearningStep)
        }
      }
    }

    patch("/:path_id/learningsteps/:step_id/?", operation(updateLearningStep)) {
      val updatedLearningStep = extract[UpdatedLearningStep](request.body)
      val createdLearningStep = updateService.updateLearningStep(long("path_id"), long("step_id"),
        updatedLearningStep,
        usernameFromHeader)

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
      updateService.updateSeqNo(long("path_id"), long("step_id"), newSeqNo.seqNo, usernameFromHeader) match {
        case Some(seqNo) => seqNo
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
      }
    }

    put("/:path_id/learningsteps/:step_id/status/?", operation(updateLearningStepStatus)) {
      val learningStepStatus = extract[LearningStepStatus](request.body)
      val stepStatus = StepStatus.valueOfOrError(learningStepStatus.status)

      val updatedStep = updateService.updateLearningStepStatus(long("path_id"), long("step_id"), stepStatus, usernameFromHeader)
      updatedStep match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} for learningpath with id ${params("path_id")} not found"))
        case Some(learningStep) => {
          logger.info(s"UPDATED LearningStep with id: ${params("step_id")} for LearningPath with id: ${params("path_id")} to STATUS = ${learningStep.status}")
          Ok(body = learningStep)
        }
      }
    }

    put("/:path_id/status/?", operation(updateLearningPathStatus)) {
      val updatedLearningPath: Option[LearningPath] = updateService.updateLearningPathStatus(
        long("path_id"),
        extract[LearningPathStatus](request.body),
        usernameFromHeader)

      updatedLearningPath match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
        case Some(learningPath) => {
          logger.info(s"UPDATED publishing status of LearningPath with ID =  ${learningPath.id}")
          Ok(body = learningPath)
        }
      }
    }

    delete("/:path_id/?", operation(deleteLearningPath)) {
      val deleted = updateService.deleteLearningPath(long("path_id"), usernameFromHeader)
      deleted match {
        case false => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
        case true => {
          logger.info(s"DELETED LearningPath with ID: ${params("path_id")}")
          halt(status = 204)
        }
      }
    }

    delete("/:path_id/learningsteps/:step_id/?", operation(deleteLearningStep)) {
      val deleted = updateService.updateLearningStepStatus(long("path_id"), long("step_id"), StepStatus.DELETED, usernameFromHeader)
      deleted match {
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} for learningpath with id ${params("path_id")} not found"))
        case Some(learningStep) => {
          logger.info(s"MARKED LearningStep with id: ${params("step_id")} for LearningPath with id: ${params("path_id")} as DELETED.")
          halt(status = 204)
        }
      }
    }

    get("/tags/?", operation(getTags)) {
      readService.tags
    }

    def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
      try {
        read[T](json)
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          throw new ValidationException(errors = List(ValidationMessage("body", e.getMessage)))
        }
      }
    }

    def optionalUsernameFromHeader(implicit request: HttpServletRequest): Option[String] = {
      request.header(UsernameHeader) match {
        case Some(h) => Some(h.replace("ndla-", ""))
        case None => None
      }
    }

    def usernameFromHeader(implicit request: HttpServletRequest): String = {
      requireHeader(UsernameHeader).get.replace("ndla-", "")
    }

    def requireHeader(headerName: String)(implicit request: HttpServletRequest): Option[String] = {
      request.header(headerName) match {
        case Some(h) => Some(h)
        case None => {
          logger.warn(s"Request made to ${request.getRequestURI} without required header $headerName.")
          throw new AccessDeniedException("You do not have access to the requested resource.")
        }
      }
    }

    def long(paramName: String)(implicit request: HttpServletRequest): Long = {
      val paramValue = params(paramName)
      paramValue.forall(_.isDigit) match {
        case true => paramValue.toLong
        case false => throw new ValidationException(errors = List(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed.")))
      }
    }

    def optLong(paramName: String)(implicit request: HttpServletRequest): Option[Long] = {
      params.get(paramName).filter(_.forall(_.isDigit)).map(_.toLong)
    }

    def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
      params.get(paramName).map(_.trim).filterNot(_.isEmpty())
    }

  }

}