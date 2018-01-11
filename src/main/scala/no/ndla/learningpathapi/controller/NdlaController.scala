/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpath.controller


import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model.api.{Error, ValidationError, ValidationMessage}
import no.ndla.learningpathapi.model.domain._
import no.ndla.network.{ApplicationUrl, AuthUser}
import no.ndla.network.model.HttpRequestException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.NativeJsonSupport

abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    ApplicationUrl.set(request)
    AuthUser.set(request)
  }

  after() {
    ApplicationUrl.clear()
    AuthUser.clear()
  }

  error {
    case v: ValidationException => halt(status = 400, body = ValidationError(messages = v.errors))
    case a: AccessDeniedException => halt(status = 403, body = Error(Error.ACCESS_DENIED, a.getMessage))
    case ole: OptimisticLockException => halt(status = 409, body = Error(Error.RESOURCE_OUTDATED, Error.RESOURCE_OUTDATED_DESCRIPTION))
    case hre: HttpRequestException => halt(status = 502, body = Error(Error.REMOTE_ERROR, hre.getMessage))
    case i: ImportException => UnprocessableEntity(body = Error(Error.IMPORT_FAILED, i.getMessage))
    case rw: ResultWindowTooLargeException => UnprocessableEntity(body = Error(Error.WINDOW_TOO_LARGE, rw.getMessage))
    case e: IndexNotFoundException => InternalServerError(body=Error.IndexMissingError)
    case t: Throwable => {
      t.printStackTrace()
      logger.error(t.getMessage)
      halt(status = 500, body = Error())
    }
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
      case None => {
        logger.warn(s"Request made to ${request.getRequestURI} without authorization")
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

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String = {
    paramOrNone(paramName).getOrElse(default)
  }

  def paramAsListOfLong(paramName: String)(implicit request: HttpServletRequest): List[Long] = {
    params.get(paramName) match {
      case None => List()
      case Some(param) => {
        val paramAsListOfStrings = param.split(",").toList.map(_.trim)
        if (!paramAsListOfStrings.forall(entry => entry.forall(_.isDigit))) {
          throw new ValidationException(errors = List(ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")))
        }
        paramAsListOfStrings.map(_.toLong)

      }
    }
  }
}
