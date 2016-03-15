package no.ndla.learningpathapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.UsernameHeader
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.api.{LearningPath, LearningPathStatus, LearningStep, _}
import no.ndla.learningpathapi.model.domain._
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}
import org.scalatra.{Ok, ScalatraServlet}

import scala.util.Try

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
      queryParam[Option[String]]("query").description("Return only Learningpaths's with content matching the specified query."),
      queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
      queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
      queryParam[Option[Int]]("page-size").description(s"The number of search hits to display for each page. Default is ${LearningpathApiProperties.DefaultPageSize}. Max page-size is ${LearningpathApiProperties.MaxPageSize}"),
      queryParam[Option[String]]("sort").description(
        """The sorting used on results.
           Default is by -relevance (desc) when querying.
           When browsing, the default is title (asc).
           The following are supported: relevance, -relevance, lastUpdated, -lastUpdated, duration, -duration, title, -title""".stripMargin)
      )
      responseMessages response500
      )

  val getPrivateLearningpaths =
    (apiOperation[List[LearningPathSummary]]("getPrivateLearningpaths")
      summary "Show your private learningpaths"
      notes "Shows your private learningpaths."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key.")
      )
      responseMessages(response403, response500)
      )

  val getLearningpath =
    (apiOperation[LearningPath]("getLearningpath")
      summary "Show details about the specified learningpath"
      notes "Shows all information about the specified learningpath."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      pathParam[String]("path_id").description("The id of the learningpath.")
      )
      responseMessages(response403, response404, response500)
      )

  val getLearningpathStatus =
    (apiOperation[LearningPathStatus]("getLearningpathStatus")
      summary "Show publishingstatus for the learningpath"
      notes "Shows publishingstatus for the learningpath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      pathParam[String]("path_id").description("The id of the learningpath.")
      )
      responseMessages(response403, response404, response500)
      )

  val getLearningsteps =
    (apiOperation[List[LearningStepSummary]]("getLearningsteps")
      summary "Show all learningsteps for given learningpath id"
      notes "Show all learningsteps for given learningpath id"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      pathParam[String]("path_id").description("The id of the learningpath.")
      )
      responseMessages(response403, response404, response500)
      )

  val getLearningstep =
    (apiOperation[LearningStep]("getLearningstep")
      summary "Show the given learningstep for the given learningpath"
      notes "Show the given learningstep for the given learningpath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      pathParam[String]("path_id").description("The id of the learningpath."),
      pathParam[String]("step_id").description("The id of the learningpath.")
      )
      responseMessages(response403, response404, response500, response502)
      )

  val addNewLearningpath =
    (
      apiOperation[LearningPath]("addLearningpath")
      summary "Adds the given learningpath"
      notes "Adds the given learningpath"
      parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key."),
        queryParam[Option[String]]("copy-from").description("Id of learningPath to use as basis for the new one."),
        bodyParam[NewLearningPath]
      )
      responseMessages(response400, response403, response404, response500)
    )


  val addNewLearningStep =
    (apiOperation[LearningStep]("addLearningStep")
      summary "Adds the given LearningStep"
      notes "Adds the given LearningStep"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      pathParam[String]("path_id").description("The id of the learningpath."),
      bodyParam[NewLearningStep]
      )
      responseMessages(response400, response403, response404, response500, response502)
      )

  val updateLearningPath =
    (apiOperation[LearningPath]("updateLearningPath")
      summary "Update the given learningpath"
      notes "Updates the given learningPath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      pathParam[String]("path_id").description("The id of the learningpath."),
      bodyParam[NewLearningPath]
      )
      responseMessages(response400, response403, response404, response500)
      )

  val updateLearningStep =
    (apiOperation[LearningStep]("updateLearningStep")
      summary "Updates the given learningStep"
      notes "Update the given learningStep"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      pathParam[String]("path_id").description("The id of the learningpath."),
      pathParam[String]("step_id").description("The id of the learningpath."),
      bodyParam[NewLearningStep]
      )
      responseMessages(response400, response403, response404, response500, response502)
      )

  val updateLearningPathStatus = (apiOperation[LearningPathStatus]("updateLearningPathStatus")
    summary "Updates the status of the given learningPath"
    notes "Updates the status of the given learningPath"
    parameters(
    headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
    headerParam[Option[String]]("app-key").description("Your app-key."),
    pathParam[String]("path_id").description("The id of the learningpath."),
    bodyParam[LearningPathStatus]
    )
    responseMessages(response400, response403, response404, response500)
    )

  val deleteLearningPath =
    (apiOperation[LearningPath]("deleteLearningPath")
    summary "Deletes the given learningPath"
    notes "Deletes the given learningPath"
    parameters(
    headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
    headerParam[Option[String]]("app-key").description("Your app-key."),
    pathParam[String]("path_id").description("The id of the learningpath.")
    )
    responseMessages(response403, response404, response500)
    )

  val deleteLearningStep =
    (apiOperation[Void]("deleteLearningStep")
    summary "Deletes the given learningStep"
    notes "Deletes the given learningStep"
    parameters(
    headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
    headerParam[Option[String]]("app-key").description("Your app-key."),
    pathParam[String]("path_id").description("The id of the learningpath."),
    pathParam[String]("step_id").description("The id of the learningpath.")
    )
    responseMessages(response403, response404, response500)
    )

  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
  }

  after() {
    LoggerContext.clearCorrelationID()
    ApplicationUrl.clear()
  }

  error{
    case h:HeaderMissingException => halt(status = 403, body = Error(Error.HEADER_MISSING, h.getMessage))
    case v:ValidationException => halt(status = 400, body = ValidationError(messages = v.errors))
    case a:AccessDeniedException => halt(status = 403, body = Error(Error.ACCESS_DENIED, a.getMessage))
    case hre: HttpRequestException => halt(status = 502, body = Error(Error.REMOTE_ERROR, hre.getMessage))
    case t:Throwable => {
      t.printStackTrace()
      logger.error(t.getMessage)
      halt(status = 500, body = Error())
    }
  }

  val publicService = ComponentRegistry.publicService
  val privateService = ComponentRegistry.privateService
  val updateService = ComponentRegistry.updateService
  val search = ComponentRegistry.searchService

  get("/", operation(getLearningpaths)) {
    val query = params.get("query")
    val language = params.get("language")
    val sort = params.get("sort")
    val pageSize = params.get("page-size").flatMap(ps => Try(ps.toInt).toOption)
    val page = params.get("page").flatMap(idx => Try(idx.toInt).toOption)
    logger.info("GET / with params query='{}', language={}, page={}, page-size={}", query, language, page, pageSize)

    query match {
      case Some(q) => search.matchingQuery(
        query = q.toLowerCase.split(" ").map(_.trim),
        language = language,
        sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
        page = page,
        pageSize = pageSize
      )
      case None => search.all(
        sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc),
        language = language,
        page = page,
        pageSize = pageSize)
    }
  }

  get("/:path_id/?", operation(getLearningpath)) {
    publicService.withId(long("path_id")) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/:path_id/status/?", operation(getLearningpathStatus)) {
    publicService.statusFor(long("path_id")) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/:path_id/learningsteps/?", operation(getLearningsteps)) {
    publicService.learningstepsFor(long("path_id")) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/:path_id/learningsteps/:step_id/?", operation(getLearningstep)) {
    publicService.learningstepFor(long("path_id"), long("step_id")) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
    }
  }

  get("/private/?", operation(getPrivateLearningpaths)) {
    privateService.all(owner = usernameFromHeader)
  }

  get ("/private/:path_id/?", operation(getLearningpath)){
    privateService.withId(long("path_id"), owner = usernameFromHeader) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/private/:path_id/status/?", operation(getLearningpathStatus)) {
    privateService.statusFor(long("path_id"), owner = usernameFromHeader) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/private/:path_id/learningsteps/?", operation(getLearningsteps)) {
    privateService.learningstepsFor(long("path_id"), owner = usernameFromHeader) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/private/:path_id/learningsteps/:step_id/?", operation(getLearningstep)) {
    privateService.learningstepFor(long("path_id"), long("step_id"), owner = usernameFromHeader) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
    }
  }

  post("/", operation(addNewLearningpath)) {
    val newLearningPath = extract[NewLearningPath](request.body).validate()
    val createdLearningPath = updateService.addLearningPath(newLearningPath, usernameFromHeader)
    logger.info(s"CREATED LearningPath with ID =  ${createdLearningPath.id}")
    halt(status = 201, headers = Map("Location" -> createdLearningPath.metaUrl), body = createdLearningPath)
  }

  put("/:path_id/?", operation(updateLearningPath)) {
    val newLearningPath = extract[NewLearningPath](request.body).validate()
    val updatedLearningPath = updateService.updateLearningPath(long("path_id"), newLearningPath, usernameFromHeader)
    updatedLearningPath match {
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      case Some(learningPath) => {
        logger.info(s"UPDATED LearningPath with ID =  ${learningPath.id}")
        Ok(body = learningPath)
      }
    }
  }

  post("/:path_id/learningsteps/?", operation(addNewLearningStep)) {
    val newLearningStep = extract[NewLearningStep](request.body).validate()
    val createdLearningStep = updateService.addLearningStep(long("path_id"), newLearningStep, usernameFromHeader)
    createdLearningStep match {
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      case Some(learningStep) => {
        logger.info(s"CREATED LearningStep with ID =  ${learningStep.id} for LearningPath with ID = ${params("path_id")}")
        halt(status = 201, headers = Map("Location" -> learningStep.metaUrl), body = createdLearningStep)
      }
    }
  }

  put("/:path_id/learningsteps/:step_id/?", operation(updateLearningStep)) {
    val newLearningStep = extract[NewLearningStep](request.body).validate()
    val createdLearningStep = updateService.updateLearningStep(long("path_id"), long("step_id"),
      newLearningStep,
      usernameFromHeader)

    createdLearningStep match {
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} for learningpath with id ${params("path_id")} not found"))
      case Some(learningStep) => {
        logger.info(s"UPDATED LearningStep with ID = ${params("step_id")} for LearningPath with ID = ${params("path_id")}")
        Ok(body = learningStep)
      }
    }
  }

  put("/:path_id/status/?", operation(updateLearningPathStatus)) {
    val learningPathStatus = extract[LearningPathStatus](request.body).validate()
    val updatedLearningPath:Option[LearningPath] = updateService.updateLearningPathStatus(
      long("path_id"),
      learningPathStatus,
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
    val deleted = updateService.deleteLearningStep(long("path_id"), long("step_id"), usernameFromHeader)
    deleted match {
      case false => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} for learningpath with id ${params("path_id")} not found"))
      case true => {
        logger.info(s"DELETED LearningStep with id: ${params("step_id")} for LearningPath with id: ${params("path_id")}")
        halt(status = 204)
      }
    }
  }

  def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    try{
      read[T](json)
    } catch {
      case e: Exception => {
        logger.error(e.getMessage, e)
        throw new ValidationException(errors = List(ValidationMessage("body", e.getMessage)))
      }
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
        throw new HeaderMissingException(s"The required header $headerName is missing.")
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

}
