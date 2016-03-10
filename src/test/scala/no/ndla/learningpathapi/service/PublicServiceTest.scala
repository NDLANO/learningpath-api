package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import no.ndla.learningpathapi.model._
import org.mockito.Mockito._
import org.mockito.Matchers._

class PublicServiceTest extends UnitSuite with TestEnvironment{

  var service: PublicService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), None, List(), List(), None, Some(1), LearningPathStatus.PUBLISHED, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), "")
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), None, List(), List(), None, Some(1), LearningPathStatus.PRIVATE, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), "")

  val STEP1 = LearningStep(Some(1), None, None, 1, List(), List(), List(), StepType.TEXT, None)
  val STEP2 = LearningStep(Some(2), None, None, 2, List(), List(), List(), StepType.TEXT, None)

  override def beforeEach() = {
    service = new PublicService
    resetMocks()

    when(authClient.getUserName(any[String])).thenReturn(NdlaUserName(Some("fornavn"), Some("mellomnavn"), Some("Etternavn")))
  }

  test("That withId returns None when id does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      service.withId(PUBLISHED_ID)
    }
  }

  test("That withId returns a learningPath when the status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))

    val learningPath = service.withId(PUBLISHED_ID)
    assert(learningPath.isDefined)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.withId(PRIVATE_ID) }.getMessage
    }
  }

  test("That statusFor returns None when id does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      service.statusFor(PUBLISHED_ID)
    }
  }

  test("That statusFor returns a LearningPathStatus when the status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("PUBLISHED") {
      service.statusFor(PUBLISHED_ID).map(_.status).get
    }
  }

  test("That statusFor throws an AccessDeniedException when the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException]{ service.statusFor(2) }.getMessage
    }
  }

  test("That learningstepsFor returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      service.learningstepsFor(PUBLISHED_ID)
    }
  }

  test("That learningstepsFor returns an empty list if the learningPath does not have any learningsteps") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PUBLISHED_ID)).thenReturn(List())
    assertResult(0) {
      service.learningstepsFor(PUBLISHED_ID).get.length
    }
  }

  test("That learningstepsFor returns all learningsteps for a learningpath") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PUBLISHED_ID)).thenReturn(List(STEP1, STEP2))
    assertResult(2) {
      service.learningstepsFor(PUBLISHED_ID).get.length
    }
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepsFor(PRIVATE_ID) }.getMessage
    }
  }

  test("That learningstepFor returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      service.learningstepFor(PUBLISHED_ID, STEP1.id.get)
    }
  }

  test("That learningstepFor returns None when the learningStep does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)
    assertResult(None) {
      service.learningstepFor(PUBLISHED_ID, STEP1.id.get)
    }
  }

  test("That learningstepFor returns the LearningStep when it exists") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service.learningstepFor(PUBLISHED_ID, STEP1.id.get).get.id
    }
  }

  test("That learningstepFor throws an AccessDeniedException when the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepFor(PRIVATE_ID, STEP1.id.get) }.getMessage
    }
  }

}
