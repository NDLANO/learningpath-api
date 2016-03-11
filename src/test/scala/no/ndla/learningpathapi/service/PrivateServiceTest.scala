package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.learningpathapi.model._
import org.mockito.Matchers._
import org.mockito.Mockito._

class PrivateServiceTest extends UnitSuite with TestEnvironment {

  var service: PrivateService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_OWNER = "eier1"
  val PRIVATE_OWNER = "eier2"

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), None, List(), List(), None, Some(1), LearningPathStatus.PUBLISHED, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PUBLISHED_OWNER)
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), None, List(), List(), None, Some(1), LearningPathStatus.PRIVATE, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PRIVATE_OWNER)

  val STEP1 = LearningStep(Some(1), None, None, 1, List(), List(), List(), StepType.TEXT, None)
  val STEP2 = LearningStep(Some(2), None, None, 2, List(), List(), List(), StepType.TEXT, None)

  override def beforeEach() = {
    service = new PrivateService
    resetMocks()

    when(authClient.getUserName(any[String])).thenReturn(NdlaUserName(Some("fornavn"), Some("mellomnavn"), Some("Etternavn")))
  }

  test("That withId returns None when id does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      service.withId(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  test("That withId returns a learningPath when the the given user is the owner. Regardless of status") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult(PRIVATE_ID) {
      service.withId(PRIVATE_ID, PRIVATE_OWNER).get.id
    }
    assertResult(PUBLISHED_ID) {
      service.withId(PUBLISHED_ID, PUBLISHED_OWNER).get.id
    }
  }

  test("That withId throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.withId(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That statusFor returns None when id does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      service.statusFor(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  test("That statusFor returns a LearningPathStatus when the given user is the owner. Regardless of status") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("PUBLISHED") {
      service.statusFor(PUBLISHED_ID, PUBLISHED_OWNER).map(_.status).get
    }
    assertResult("PRIVATE") {
      service.statusFor(PRIVATE_ID, PRIVATE_OWNER).map(_.status).get
    }
  }

  test("That statusFor throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.statusFor(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That learningstepsFor returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      service.learningstepsFor(PRIVATE_ID, PRIVATE_OWNER)
    }
  }

  test("That learningStepsFor returns an empty list if the learningPath does not have any learningsteps when the given user is the owner. Regardless of status") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PUBLISHED_ID)).thenReturn(List())
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PRIVATE_ID)).thenReturn(List())
    assertResult(0) {
      service.learningstepsFor(PUBLISHED_ID, PUBLISHED_OWNER).get.length
    }
    assertResult(0) {
      service.learningstepsFor(PRIVATE_ID, PRIVATE_OWNER).get.length
    }
  }

  test("That learningStepsFor returns all learningsteps for a learningpath when the given user is the owner. Regardless of status") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PUBLISHED_ID)).thenReturn(List(STEP1, STEP2))
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PRIVATE_ID)).thenReturn(List(STEP1))
    assertResult(2) {
      service.learningstepsFor(PUBLISHED_ID, PUBLISHED_OWNER).get.length
    }
    assertResult(1) {
      service.learningstepsFor(PRIVATE_ID, PRIVATE_OWNER).get.length
    }
  }

  test("That learningStepsFor throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepsFor(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That learningstepFor returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      service.learningstepFor(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
  }

  test("That learningstepFor returns None when the learningStep does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(None)
    assertResult(None) {
      service.learningstepFor(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
  }

  test("That learningstepFor returns the LearningStep when it exists and the given user is the owner. Regardless of status") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP2.id.get)).thenReturn(Some(STEP2))
    assertResult(STEP1.id.get) {
      service.learningstepFor(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER).get.id
    }
    assertResult(STEP2.id.get) {
      service.learningstepFor(PRIVATE_ID, STEP2.id.get, PRIVATE_OWNER).get.id
    }
  }

  test("That learningstepFor throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepFor(PRIVATE_ID, STEP1.id.get, PUBLISHED_OWNER) }.getMessage
    }
  }
}
