package no.ndla.learningpathapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.UsernameHeader
import no.ndla.learningpathapi.model.{Error, HeaderMissingException}
import no.ndla.learningpathapi.service.LearningpathService
import no.ndla.learningpathapi._
import no.ndla.logging.LoggerContext
import no.ndla.network
import no.ndla.network.ApplicationUrl
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}
import org.scalatra.{Ok, ScalatraServlet}

import scala.Error

class LearningpathController(implicit val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  protected val applicationDescription = "API for accessing Learningpaths from ndla.no."
  val getLearningpaths =
    (apiOperation[List[LearningPathSummary]]("getLearningpaths")
      summary "Show all learningpaths"
      notes "Shows all the learningpaths."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("query").description("Return only Learningpaths's with content matching the specified query."),
      queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
      queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
      queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
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
      bodyParam[LearningPath]
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
    case t:Throwable => {
      logger.error(t.getMessage)
      halt(status = 500, body = Error.GenericError)
    }
  }

  val publics = new LearningpathService(LearningpathApiProperties.Published)
  val privates = new LearningpathService(LearningpathApiProperties.Private)

  // PUBLIC GET
  get("/", operation(getLearningpaths)) {
    publics.all()
  }

  get("/:path_id", operation(getLearningpath)) {
    publics.withId(params("path_id")) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/:path_id/status", operation(getLearningpathStatus)) {
    publics.statusFor(params("path_id")) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/:path_id/learningsteps", operation(getLearningsteps)) {
    publics.learningstepsFor(params("path_id")) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/:path_id/learningsteps/:step_id", operation(getLearningstep)) {
    publics.learningstepFor(params("path_id"), params("step_id")) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
    }
  }

  // PRIVATE GET
  get("/private", operation(getLearningpaths)) {
    val owner = requireHeader(UsernameHeader)
    logger.info("GET /private with params X-Consumer-Username='{}'", owner)
    privates.all(owner = owner)
  }

  get ("/private/:path_id", operation(getLearningpath)){
    val owner = requireHeader(UsernameHeader)
    logger.info(s"GET /private/${params("path_id")} with params X-Consumer-Username='{}'", owner)
    privates.withId(params("path_id"), owner = owner) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/private/:path_id/status", operation(getLearningpathStatus)) {
    val owner = requireHeader(UsernameHeader)
    logger.info(s"GET /private/${params("path_id")}/status with params X-Consumer-Username='{}'", owner)

    privates.statusFor(params("path_id"), owner = owner) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/private/:path_id/learningsteps", operation(getLearningsteps)) {
    val owner = requireHeader(UsernameHeader)
    logger.info(s"GET /private/${params("path_id")}/learningsteps with params X-Consumer-Username='{}'", owner)

    privates.learningstepsFor(params("path_id"), owner = owner) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningpath with id ${params("path_id")} not found"))
    }
  }

  get("/private/:path_id/learningsteps/:step_id", operation(getLearningstep)) {
    val owner = requireHeader(UsernameHeader)
    logger.info(s"GET /private/${params("path_id")}/learningsteps/${params("step_id")} with params X-Consumer-Username='{}'", owner)

    privates.learningstepFor(params("path_id"), params("step_id"), owner = requireHeader(UsernameHeader)) match {
      case Some(x) => x
      case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Learningstep with id ${params("step_id")} not found for learningpath with id ${params("path_id")}"))
    }
  }

  // ADD ELEMENTS
  post("/", operation(addNewLearningpath)) {
    logger.info(s"ADD LEARNINGPATH = ${request.body}")
    Ok(body = request.body)
  }

  post("/:path_id/learningsteps/") {
    logger.info(s"ADD LEARNINGSTEP = ${request.body}")
    Ok(body = request.body)
  }

  // UPDATE ELEMENTS
  put("/:path_id/") {
    logger.info(s"UPDATE LEARNINGPATH = ${request.body}")
    Ok(body = request.body)
  }

  put("/:path_id/learningsteps/:step_id/") {
    logger.info(s"UPDATE LEARNINGSTEP = ${request.body}")
    Ok(body = request.body)
  }

  put("/:path_id/status/") {
    logger.info(s"UPDATE PUBLISHSTATUS = ${request.body}")
    Ok(body = request.body)
  }

  // DELETE ELEMENTS
  delete("/:path_id") {
    logger.info(s"DELETE LEARNINGPATH ID: ${params.get("path_id")}")
    halt(status = 204)
  }

  delete("/:path_id/learningsteps/:step_id") {
    logger.info(s"DELETE LEARNINGPATH ID: ${params.get("path_id")} AND STEP ID: ${params.get("step_id")}")
    halt(status = 204)
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

}
