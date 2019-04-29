/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.controller

import java.util.Date

import no.ndla.learningpathapi.model.api.config.{ConfigMeta, UpdateConfigValue}
import no.ndla.learningpathapi.model.domain.UserInfo
import no.ndla.learningpathapi.model.domain.config.ConfigKey
import no.ndla.learningpathapi.{LearningpathSwagger, TestEnvironment, UnitSuite}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite
import no.ndla.learningpathapi.TestData._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

class ConfigControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  implicit val swagger: LearningpathSwagger = new LearningpathSwagger

  lazy val controller = new ConfigController
  addServlet(controller, "/*")

  override def beforeEach(): Unit = {
    resetMocks()
    when(languageValidator.validate(any[String], any[String], any[Boolean]))
      .thenReturn(None)
  }

  test("That updating config returns 200 if all is good") {
    when(updateService.updateConfig(any[ConfigKey.Value], any[UpdateConfigValue], any[UserInfo]))
      .thenReturn(Success(ConfigMeta(ConfigKey.IsExamPeriod.toString, value = true, new Date(), "someoneCool")))

    post(s"/${ConfigKey.IsExamPeriod}",
         body = "{\"value\": true}",
         headers = Map("Authorization" -> s"Bearer $adminScopeClientToken")) {
      status should be(200)
    }

    post(s"/${ConfigKey.IsExamPeriod}",
         body = "{\"value\": true}",
         headers = Map("Authorization" -> s"Bearer $adminAndWriteScopeClientToken")) {
      status should be(200)
    }
  }
}
