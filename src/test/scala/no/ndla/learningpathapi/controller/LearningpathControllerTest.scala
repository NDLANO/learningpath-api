package no.ndla.learningpathapi.controller


import javax.servlet.http.HttpServletRequest

import no.ndla.learningpathapi.model.AccessDeniedException
import no.ndla.learningpathapi.{UnitSuite, LearningpathApiProperties, LearningpathSwagger}
import org.mockito.Mockito._

class LearningpathControllerTest extends UnitSuite {

  implicit val swagger = new LearningpathSwagger
  var controller:LearningpathController = _


  override def beforeEach() = {
    controller = new LearningpathController
  }

  test("That requireHeader returns header value when header exists") {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader("username")).thenReturn("verdi")
    assertResult(Some("verdi")) {
      controller.requireHeader("username")
    }
  }

  test("That requireHeader throws HeaderMissingException if heades does not exist") {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader("username")).thenReturn(null)
    when(request.getRequestURI).thenReturn(""
    )
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { controller.requireHeader("username") }.getMessage
    }
  }

  test("That usernameFromHeader replaces ndla- in the header value") {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader(LearningpathApiProperties.UsernameHeader)).thenReturn("ndla-123-123-123")
    assertResult("123-123-123") {
      controller.usernameFromHeader
    }
  }

  test("That usernameFromHeader does not replace anything else than ndla-") {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader(LearningpathApiProperties.UsernameHeader)).thenReturn("someotherword-123-123-123")
    assertResult("someotherword-123-123-123") {
      controller.usernameFromHeader
    }
  }
}
