package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.UnitSpec
import no.ndla.learningpathapi.business.LearningpathData
import no.ndla.learningpathapi.model.{LearningStep, LearningPath, AccessDeniedException}
import org.mockito.Mockito._

class PrivateServiceTest extends UnitSpec {

  var learningPathDataMock: LearningpathData = _
  var privateService: PrivateService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_OWNER = "eier1"
  val PRIVATE_OWNER = "eier2"

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), List(), List(), None, 1, "PUBLISHED", "", new Date(), List(), PUBLISHED_OWNER)
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), List(), List(), None, 1, "PRIVATE", "", new Date(), List(), PRIVATE_OWNER)

  val STEP1 = LearningStep(Some(1), None, 1, List(), List(), List(), "", None)
  val STEP2 = LearningStep(Some(2), None, 2, List(), List(), List(), "", None)

  override def beforeEach() = {
    learningPathDataMock = mock[LearningpathData]
    privateService = new PrivateService(learningPathDataMock)
  }

  "PrivateService.withId" should "return None when id does not exist" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      privateService.withId(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  it should "return a learningPath when the the given user is the owner. Regardless of status" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult(PRIVATE_ID) {
      privateService.withId(PRIVATE_ID, PRIVATE_OWNER).get.id
    }
    assertResult(PUBLISHED_ID) {
      privateService.withId(PUBLISHED_ID, PUBLISHED_OWNER).get.id
    }
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { privateService.withId(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  "PrivateService.statusFor" should "return None when id does not exist" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      privateService.statusFor(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  it should "return a LearningPathStatus when the given user is the owner. Regardless of status" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("PUBLISHED") {
      privateService.statusFor(PUBLISHED_ID, PUBLISHED_OWNER).map(_.status).get
    }
    assertResult("PRIVATE") {
      privateService.statusFor(PRIVATE_ID, PRIVATE_OWNER).map(_.status).get
    }
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { privateService.withId(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  "PrivateService.learningstepsFor" should "return None when the learningPath does not exist" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      privateService.learningstepsFor(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  it should "return an empty list if the learningPath does not have any learningsteps when the given user is the owner. Regardless of status" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepsFor(PUBLISHED_ID)).thenReturn(List())
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepsFor(PRIVATE_ID)).thenReturn(List())
    assertResult(0) {
      privateService.learningstepsFor(PUBLISHED_ID, PUBLISHED_OWNER).get.length
    }
    assertResult(0) {
      privateService.learningstepsFor(PRIVATE_ID, PRIVATE_OWNER).get.length
    }
  }

  it should "return return all learningsteps for a learningpath when the given user is the owner. Regardless of status" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepsFor(PUBLISHED_ID)).thenReturn(List(STEP1, STEP2))
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepsFor(PRIVATE_ID)).thenReturn(List(STEP1))
    assertResult(2) {
      privateService.learningstepsFor(PUBLISHED_ID, PUBLISHED_OWNER).get.length
    }
    assertResult(1) {
      privateService.learningstepsFor(PRIVATE_ID, PRIVATE_OWNER).get.length
    }
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { privateService.learningstepsFor(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  "PrivateService.learningstepFor" should "return None when the learningPath does not exist" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      privateService.learningstepFor(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
  }

  it should "return None when the learningStep does not exist" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(None)
    assertResult(None) {
      privateService.learningstepFor(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
  }

  it should "return the LearningStep when it exists and the given user is the owner. Regardless of status" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP2.id.get)).thenReturn(Some(STEP2))
    assertResult(STEP1.id.get) {
      privateService.learningstepFor(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER).get.id
    }
    assertResult(STEP2.id.get) {
      privateService.learningstepFor(PRIVATE_ID, STEP2.id.get, PRIVATE_OWNER).get.id
    }
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { privateService.learningstepFor(PRIVATE_ID, STEP1.id.get, PUBLISHED_OWNER) }.getMessage
    }
  }
}
