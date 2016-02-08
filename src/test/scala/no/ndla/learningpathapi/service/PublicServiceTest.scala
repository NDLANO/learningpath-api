package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.UnitSpec
import no.ndla.learningpathapi.business.LearningpathData
import no.ndla.learningpathapi.model.{LearningStep, AccessDeniedException, LearningPath}
import org.mockito.Matchers._
import org.mockito.Mockito._

class PublicServiceTest extends UnitSpec {

  var learningPathDataMock: LearningpathData = _
  var publicService: PublicService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), List(), List(), None, 1, "PUBLISHED", "", new Date(), List(), "")
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), List(), List(), None, 1, "PRIVATE", "", new Date(), List(), "")

  val STEP1 = LearningStep(Some(1), None, 1, List(), List(), List(), "", None)
  val STEP2 = LearningStep(Some(2), None, 2, List(), List(), List(), "", None)

  override def beforeEach() = {
    learningPathDataMock = mock[LearningpathData]
    publicService = new PublicService(learningPathDataMock)
  }

  "PublicService.withId" should "return None when id does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      publicService.withId(PUBLISHED_ID)
    }
  }

  it should "return a learningPath when the status is PUBLISHED" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))

    val learningPath = publicService.withId(PUBLISHED_ID)
    assert(learningPath.isDefined)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  it should "throw an AccessDeniedException when the status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { publicService.withId(PRIVATE_ID) }.getMessage
    }
  }

  "PublicService.statusFor" should "return None when id does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      publicService.statusFor(PUBLISHED_ID)
    }
  }

  it should "return a LearningPathStatus when the status is PUBLISHED" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("PUBLISHED") {
      publicService.statusFor(PUBLISHED_ID).map(_.status).get
    }
  }

  it should "throw an AccessDeniedException when the status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException]{ publicService.statusFor(2) }.getMessage
    }
  }

  "PublicService.learningstepsFor" should "return None when the learningPath does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      publicService.learningstepsFor(PUBLISHED_ID)
    }
  }

  it should "return an empty list if the learningPath does not have any learningsteps" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepsFor(PUBLISHED_ID)).thenReturn(List())
    assertResult(0) {
      publicService.learningstepsFor(PUBLISHED_ID).get.length
    }
  }

  it should "return return all learningsteps for a learningpath" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepsFor(PUBLISHED_ID)).thenReturn(List(STEP1, STEP2))
    assertResult(2) {
      publicService.learningstepsFor(PUBLISHED_ID).get.length
    }
  }

  it should "throw an AccessDeniedException when the status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { publicService.learningstepsFor(PRIVATE_ID) }.getMessage
    }
  }

  "PublicService.learningstepFor" should "return None when the learningPath does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      publicService.learningstepFor(PUBLISHED_ID, STEP1.id.get)
    }
  }

  it should "return None when the learningStep does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)
    assertResult(None) {
      publicService.learningstepFor(PUBLISHED_ID, STEP1.id.get)
    }
  }

  it should "return the LearningStep when it exists" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      publicService.learningstepFor(PUBLISHED_ID, STEP1.id.get).get.id
    }
  }

  it should "throw an AccessDeniedException when the status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { publicService.learningstepFor(PRIVATE_ID, STEP1.id.get) }.getMessage
    }
  }

}
