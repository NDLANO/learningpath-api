package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.ResponseMessage

case class ResponseMessageWithModel(code: Int, message: String, responseModel: String) extends ResponseMessage[String]