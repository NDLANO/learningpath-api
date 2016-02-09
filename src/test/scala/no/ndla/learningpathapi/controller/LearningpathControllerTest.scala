package no.ndla.learningpathapi.controller


import javax.servlet.http.HttpServletRequest

import no.ndla.learningpathapi.model.{HeaderMissingException, AccessDeniedException}
import no.ndla.learningpathapi.{LearningpathApiProperties, LearningpathSwagger, UnitSpec}
import org.mockito.Mockito._

class LearningpathControllerTest extends UnitSpec {

  implicit val swagger = new LearningpathSwagger
  var controller:LearningpathController = _


  override def beforeEach() = {
    controller = new LearningpathController
  }

  "requireHeader" should "return header value when header exists" in {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader("username")).thenReturn("verdi")
    assertResult(Some("verdi")) {
      controller.requireHeader("username")
    }
  }

  it should "throw HeaderMissingException if heades does not exist" in {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader("username")).thenReturn(null)
    when(request.getRequestURI).thenReturn(""
    )
    assertResult("The required header username is missing.") {
      intercept[HeaderMissingException] { controller.requireHeader("username") }.getMessage
    }
  }

  "usernameFromHeader" should "replace ndla- in the header value" in {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader(LearningpathApiProperties.UsernameHeader)).thenReturn("ndla-123-123-123")
    assertResult("123-123-123") {
      controller.usernameFromHeader
    }
  }

  it should "not replace anything else than ndla-" in {
    implicit val request:HttpServletRequest = mock[HttpServletRequest]
    when(request.getHeader(LearningpathApiProperties.UsernameHeader)).thenReturn("someotherword-123-123-123")
    assertResult("someotherword-123-123-123") {
      controller.usernameFromHeader
    }
  }
}
