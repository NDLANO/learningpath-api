package no.ndla.learningpathapi.service

import java.util.Date
import javax.servlet.http.HttpServletRequest

import no.ndla.learningpathapi.model.api.{Author, LearningPath}
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl

import org.mockito.Mockito._

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val apiLearningPath = LearningPath(1, List(), List(), "", List(), "", None, Some(1), "PRIVATE", "", new Date(), List(), Author("", ""))

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
}
