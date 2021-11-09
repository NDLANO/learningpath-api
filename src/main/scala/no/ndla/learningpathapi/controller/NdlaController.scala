/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import javax.servlet.http.HttpServletRequest
import no.ndla.learningpathapi.ComponentRegistry
import no.ndla.learningpathapi.model.api.{Error, ImportReport, ValidationError, ValidationMessage}
import no.ndla.learningpathapi.model.domain._
import no.ndla.network.model.HttpRequestException
import no.ndla.network.{ApplicationUrl, AuthUser}
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.postgresql.util.PSQLException
import org.scalatra._
import org.scalatra.json.NativeJsonSupport

import scala.util.Try

abstract class NdlaController
    extends ScalatraServlet
    with NativeJsonSupport
    with LazyLogging
    with CorrelationIdSupport {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    ApplicationUrl.set(request)
    AuthUser.set(request)
    logger.info("{} {}{}",
                request.getMethod,
                request.getRequestURI,
                Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
  }

  after() {
    ApplicationUrl.clear()
    AuthUser.clear()
  }

  error {
    case v: ValidationException =>
      halt(status = 400, body = ValidationError(messages = v.errors))
    case a: AccessDeniedException =>
      halt(status = 403, body = Error(Error.ACCESS_DENIED, a.getMessage))
    case ole: OptimisticLockException =>
      halt(status = 409, body = Error(Error.RESOURCE_OUTDATED, Error.RESOURCE_OUTDATED_DESCRIPTION))
    case nfe: NotFoundException =>
      halt(status = 404, body = Error(Error.NOT_FOUND, nfe.getMessage))
    case hre: HttpRequestException =>
      halt(status = 502, body = Error(Error.REMOTE_ERROR, hre.getMessage))
    case i: ImportException =>
      UnprocessableEntity(body = Error(Error.IMPORT_FAILED, i.getMessage))
    case rw: ResultWindowTooLargeException =>
      UnprocessableEntity(body = Error(Error.WINDOW_TOO_LARGE, rw.getMessage))
    case e: IndexNotFoundException =>
      InternalServerError(body = Error.IndexMissingError)
    case i: ElasticIndexingException =>
      InternalServerError(body = Error(Error.GENERIC, i.getMessage))
    case ir: ImportReport => UnprocessableEntity(body = ir)
    case _: PSQLException =>
      ComponentRegistry.connectToDatabase()
      InternalServerError(Error.DatabaseUnavailableError)
    case mse: InvalidStatusException =>
      BadRequest(Error(Error.MISSING_STATUS, mse.getMessage))
    case nse: NdlaSearchException
        if nse.rf.error.rootCause.exists(x =>
          x.`type` == "search_context_missing_exception" || x.reason == "Cannot parse scroll id") =>
      BadRequest(body = Error.InvalidSearchContext)
    case t: Throwable =>
      t.printStackTrace()
      logger.error(t.getMessage)
      halt(status = 500, body = Error())
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

  def requireUserId(implicit request: HttpServletRequest): String = {
    AuthUser.get match {
      case Some(user) => user
      case None =>
        logger.warn(s"Request made to ${request.getRequestURI} without authorization")
        throw AccessDeniedException("You do not have access to the requested resource.")
    }
  }

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    paramValue.forall(_.isDigit) match {
      case true => paramValue.toLong
      case false =>
        throw new ValidationException(
          errors = List(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed.")))
    }
  }

  def optLong(paramName: String)(implicit request: HttpServletRequest): Option[Long] = {
    params.get(paramName).filter(_.forall(_.isDigit)).map(_.toLong)
  }

  def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
    params.get(paramName).map(_.trim).filterNot(_.isEmpty())
  }

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String = {
    paramOrNone(paramName).getOrElse(default)
  }

  def intOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Int] =
    paramOrNone(paramName).flatMap(p => Try(p.toInt).toOption)

  def intOrDefault(paramName: String, default: Int): Int =
    intOrNone(paramName).getOrElse(default)

  def booleanOrNone(paramName: String)(implicit request: HttpServletRequest): Option[Boolean] =
    paramOrNone(paramName).flatMap(p => Try(p.toBoolean).toOption)

  def booleanOrDefault(paramName: String, default: Boolean)(implicit request: HttpServletRequest): Boolean =
    booleanOrNone(paramName).getOrElse(default)

  def paramAsListOfString(paramName: String)(implicit request: HttpServletRequest): List[String] = {
    params.get(paramName) match {
      case None        => List.empty
      case Some(param) => param.split(",").toList.map(_.trim)
    }
  }

  def paramAsListOfLong(paramName: String)(implicit request: HttpServletRequest): List[Long] = {
    params.get(paramName) match {
      case None => List()
      case Some(param) => {
        val paramAsListOfStrings = param.split(",").toList.map(_.trim)
        if (!paramAsListOfStrings.forall(entry => entry.forall(_.isDigit))) {
          throw new ValidationException(
            errors =
              List(ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")))
        }
        paramAsListOfStrings.map(_.toLong)

      }
    }
  }

  def doOrAccessDenied(hasAccess: Boolean, reason: String = "Missing user/client-id or role")(w: => Any): Any = {
    if (hasAccess) {
      w
    } else {
      errorHandler(AccessDeniedException(reason))
    }
  }
}
