package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.UnitSuite
import no.ndla.learningpathapi.business.LearningpathData
import no.ndla.learningpathapi.model._
import org.mockito.Mockito._

class PrivateServiceTest extends UnitSuite {

  var learningPathDataMock: LearningpathData = _
  var privateService: PrivateService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_OWNER = "eier1"
  val PRIVATE_OWNER = "eier2"

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), None, List(), List(), None, 1, LearningPathStatus.PUBLISHED, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PUBLISHED_OWNER)
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), None, List(), List(), None, 1, LearningPathStatus.PRIVATE, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PRIVATE_OWNER)

  val STEP1 = LearningStep(Some(1), None, None, 1, List(), List(), List(), StepType.TEXT, None)
  val STEP2 = LearningStep(Some(2), None, None, 2, List(), List(), List(), StepType.TEXT, None)

  override def beforeEach() = {
    learningPathDataMock = mock[LearningpathData]
    privateService = new PrivateService(learningPathDataMock)
  }

  test("That withId returns None when id does not exist") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      privateService.withId(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  test("That withId returns a learningPath when the the given user is the owner. Regardless of status") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult(PRIVATE_ID) {
      privateService.withId(PRIVATE_ID, PRIVATE_OWNER).get.id
    }
    assertResult(PUBLISHED_ID) {
      privateService.withId(PUBLISHED_ID, PUBLISHED_OWNER).get.id
    }
  }

  test("That withId throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { privateService.withId(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That statusFor returns None when id does not exist") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      privateService.statusFor(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  test("That statusFor returns a LearningPathStatus when the given user is the owner. Regardless of status") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("PUBLISHED") {
      privateService.statusFor(PUBLISHED_ID, PUBLISHED_OWNER).map(_.status).get
    }
    assertResult("PRIVATE") {
      privateService.statusFor(PRIVATE_ID, PRIVATE_OWNER).map(_.status).get
    }
  }

  test("That statusFor throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { privateService.statusFor(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That learningstepsFor returns None when the learningPath does not exist") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      privateService.learningstepsFor(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  test("That learningStepsFor returns an empty list if the learningPath does not have any learningsteps when the given user is the owner. Regardless of status") {
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

  test("That learningStepsFor returns all learningsteps for a learningpath when the given user is the owner. Regardless of status") {
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

  test("That learningStepsFor throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { privateService.learningstepsFor(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That learningstepFor returns None when the learningPath does not exist") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      privateService.learningstepFor(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
  }

  test("That learningstepFor returns None when the learningStep does not exist") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(None)
    assertResult(None) {
      privateService.learningstepFor(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
  }

  test("That learningstepFor returns the LearningStep when it exists and the given user is the owner. Regardless of status") {
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

  test("That learningstepFor throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { privateService.learningstepFor(PRIVATE_ID, STEP1.id.get, PUBLISHED_OWNER) }.getMessage
    }
  }
}
