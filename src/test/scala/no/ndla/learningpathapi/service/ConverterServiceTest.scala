/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date
import javax.servlet.http.HttpServletRequest

import no.ndla.learningpathapi.model.api.{Author, Copyright, LearningPath, License}
import no.ndla.learningpathapi.model.domain.{Description, LearningStep, StepType}
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import no.ndla.network.ApplicationUrl
import org.mockito.Mockito._

class ConverterServiceTest extends UnitSuite with UnitTestEnvironment {
  val clinton = Author("author", "Crooked Hillary")
  val license = License("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
  val copyright = Copyright(license, List(clinton))
  val apiLearningPath = LearningPath(1, 1, None, List(), List(), "", List(), "", None, Some(1), "PRIVATE", "", new Date(), List(), Author("", ""), copyright, true)
  val domainLearningStep = LearningStep(None, None, None, None, 1, List(), List(), List(), StepType.INTRODUCTION, None)
  var service: ConverterService = _

  override def beforeEach() = {
    service = new ConverterService
  }

  test("That createUrlToLearningPath does not include private in path for private learningpath") {
    val httpServletRequest = mock[HttpServletRequest]
    when(httpServletRequest.getServerPort).thenReturn(80)
    when(httpServletRequest.getScheme).thenReturn("http")
    when(httpServletRequest.getServerName).thenReturn("localhost")
    when(httpServletRequest.getServletPath).thenReturn("/servlet")

    ApplicationUrl.set(httpServletRequest)
    service.createUrlToLearningPath(apiLearningPath.copy(status = "PRIVATE")) should equal("http://localhost/servlet/1")
  }

  test("That asApiIntroduction returns an introduction for a given step") {
    val introductions = service.asApiIntroduction(Some(domainLearningStep.copy(description = List(
      Description("Introduksjon på bokmål", Some("nb")),
      Description("Introduksjon på nynorsk", Some("nn")),
      Description("Introduction in english", Some("en"))
    ))))

    introductions.size should be (3)
    introductions.find(_.language.contains("nb")).map(_.introduction) should be (Some("Introduksjon på bokmål"))
    introductions.find(_.language.contains("nn")).map(_.introduction) should be (Some("Introduksjon på nynorsk"))
    introductions.find(_.language.contains("en")).map(_.introduction) should be (Some("Introduction in english"))
  }

  test("That asApiIntroduction returns empty list if no descriptions are available") {
    val introductions = service.asApiIntroduction(Some(domainLearningStep))
    introductions.size should be (0)
  }

  test("That asApiIntroduction returns an empty list if given a None") {
    service.asApiIntroduction(None) should equal(List())
  }

  test("asApiLicense returns a License object for a given valid license") {
    service.asApiLicense("by") should equal(License("by", Option("Creative Commons Attribution 2.0 Generic"), Some("https://creativecommons.org/licenses/by/2.0/")))
  }

  test("asApiLicense returns a default license object for an invalid license") {
    service.asApiLicense("invalid") should equal(License("invalid", Option("Invalid license"), None))
  }
}
