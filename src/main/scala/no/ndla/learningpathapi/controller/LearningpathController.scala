package no.ndla.learningpathapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.UsernameHeader
import no.ndla.learningpathapi.{LearningPath, LearningStep, _}
import no.ndla.learningpathapi.integration.AmazonIntegration
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.service.{PublicService, PrivateService, UpdateService}
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl
import org.json4s.native.Serialization.read
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}
import org.scalatra.{Ok, ScalatraServlet}

import scala.util.Try

class LearningpathController(implicit val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats + new EnumNameSerializer(Error)


  protected val applicationDescription = "API for accessing Learningpaths from ndla.no."
  val getLearningpaths =
    (apiOperation[SearchResult]("getLearningpaths")
      summary "Show public learningpaths"
      notes "Shows public learningpaths."
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
      )
  val getPrivateLearningpaths =
    (apiOperation[List[LearningPathSummary]]("getPrivateLearningpaths")
      summary "Show your private learningpaths"
      notes "Shows your private learningpaths."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key.")
      )
      )

  val getLearningpath =
    (apiOperation[LearningPath]("getLearningpath")
      summary "Show details about the specified learningpath"
      notes "Shows all information about the specified learningpath."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("path_id").description("The id of the learningpath.")
      )
      )

  val getLearningpathStatus =
    (apiOperation[LearningPathStatus]("getLearningpathStatus")
      summary "Show publishingstatus for the learningpath"
      notes "Shows publishingstatus for the learningpath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("path_id").description("The id of the learningpath.")
      )
      )

  val getLearningsteps =
    (apiOperation[List[LearningStep]]("getLearningsteps")
      summary "Show all learningsteps for given learningpath id"
      notes "Show all learningsteps for given learningpath id"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("path_id").description("The id of the learningpath.")
      )
      )

  val getLearningstep =
    (apiOperation[LearningStep]("getLearningstep")
      summary "Show the given learningstep for the given learningpath"
      notes "Show the given learningstep for the given learningpath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("path_id").description("The id of the learningpath."),
      queryParam[Option[String]]("step_id").description("The id of the learningpath.")
      )
      )

  val addNewLearningpath =
    (apiOperation[LearningPath]("addLearningpath")
      summary "Adds the given learningpath"
      notes "Adds the given learningpath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("copy-from").description("Id of learningPath to use as basis for the new one."),
      bodyParam[NewLearningPath]
      )
      )

  val addNewLearningStep =
    (apiOperation[LearningStep]("addLearningStep")
      summary "Adds the given LearningStep"
      notes "Adds the given LearningStep"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      bodyParam[NewLearningStep]
      )
      )

  val updateLearningPath =
    (apiOperation[LearningPath]("updateLearningPath")
      summary "Update the given learningpath"
      notes "Updates the given learningPath"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      bodyParam[NewLearningPath]
      )
      )

  val updateLearningStep =
    (apiOperation[LearningStep]("updateLearningStep")
      summary "Updates the given learningStep"
      notes "Update the given learningStep"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      bodyParam[NewLearningStep]
      )
      )

  val updateLearningPathStatus = (apiOperation[LearningPathStatus]("updateLearningPathStatus")
    summary "Updates the status of the given learningPath"
    notes "Updates the status of the given learningPath"
    parameters(
    headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
    headerParam[Option[String]]("app-key").description("Your app-key."),
    bodyParam[LearningPathStatus]
    )
    )

  val deleteLearningPath =
    (apiOperation[LearningPath]("deleteLearningPath")
    summary "Deletes the given learningPath"
    notes "Deletes the given learningPath"
    parameters(
    headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
    headerParam[Option[String]]("app-key").description("Your app-key.")
    )
    )

  val deleteLearningStep =
    (apiOperation[Void]("deleteLearningStep")
    summary "Deletes the given learningStep"
    notes "Deletes the given learningStep"
    parameters(
    headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
    headerParam[Option[String]]("app-key").description("Your app-key.")
    )
    )

  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
  }

  after() {
    LoggerContext.clearCorrelationID
    ApplicationUrl.clear()
  }

  error{
    case h:HeaderMissingException => halt(status = 403, body = Error(Error.HEADER_MISSING, h.getMessage))
    case v:ValidationException => halt(status = 400, body = Error(Error.VALIDATION, v.getMessage))
    case a:AccessDeniedException => halt(status = 403, body = Error(Error.ACCESS_DENIED, a.getMessage))
    case t:Throwable => {
      t.printStackTrace()
      logger.error(t.getMessage)
      halt(status = 500, body = Error.GenericError)
    }
  }

  val publicService = AmazonIntegration.getPublicService()
  val privateService = AmazonIntegration.getPrivateService()
  val search = AmazonIntegration.getLearningPathSearch()
  val updateService = AmazonIntegration.getUpdateService()

  get("/", operation(getLearningpaths)) {
    val query = params.get("query")
    val language = params.get("language")
    val sort = params.get("sort")
    val pageSize = params.get("page-size").flatMap(ps => Try(ps.toInt).toOption)
    val page = params.get("page").flatMap(idx => Try(idx.toInt).toOption)
    logger.info("GET / with params query='{}', language={}, page={}, page-size={}", query, language, page, pageSize)


    query match {
      case Some(q) => search.matchingQuery(
        query = q.toLowerCase().split(" ").map(_.trim),
        language = language,
        sort = Sort.valueOf(sort).getOrElse(Sort.ByRelevanceDesc),
        page = page,
        pageSize = pageSize
      )
      case None => search.all(sort = Sort.valueOf(sort).getOrElse(Sort.ByTitleAsc), language = language, page = page, pageSize = pageSize)
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
    val createdLearningPath = updateService.addLearningPath(read[NewLearningPath](request.body), usernameFromHeader)
    logger.info(s"CREATED LearningPath with ID =  ${createdLearningPath.id}")
    halt(status = 201, headers = Map("Location" -> createdLearningPath.metaUrl), body = createdLearningPath)
  }

  put("/:path_id/?", operation(updateLearningPath)) {
    val updatedLearningPath = updateService.updateLearningPath(long("path_id"), read[NewLearningPath](request.body), usernameFromHeader)
    updatedLearningPath match {
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      case Some(learningPath) => {
        logger.info(s"UPDATED LearningPath with ID =  ${learningPath.id}")
        Ok(body = learningPath)
      }
    }
  }

  post("/:path_id/learningsteps/?", operation(addNewLearningStep)) {
    val createdLearningStep = updateService.addLearningStep(long("path_id"), read[NewLearningStep](request.body), usernameFromHeader)
    createdLearningStep match {
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
      case Some(learningStep) => {
        logger.info(s"CREATED LearningStep with ID =  ${learningStep.id} for LearningPath with ID = ${params("path_id")}")
        halt(status = 201, headers = Map("Location" -> learningStep.metaUrl), body = createdLearningStep)
      }
    }
  }

  put("/:path_id/learningsteps/:step_id/?", operation(updateLearningStep)) {
    val createdLearningStep = updateService.updateLearningStep(long("path_id"), long("step_id"),
      read[NewLearningStep](request.body),
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
    val updatedLearningPath:Option[LearningPath] = updateService.updateLearningPathStatus(
      long("path_id"),
      read[LearningPathStatus](request.body),
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
      case false => throw new ValidationException(s"Invalid value for $paramName. Only digits are allowed.")
    }
  }

}
