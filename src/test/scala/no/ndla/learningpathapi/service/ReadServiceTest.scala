package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite, UnitTestEnvironment}
import no.ndla.learningpathapi.model._
import org.mockito.Mockito._
import org.mockito.Matchers._

class ReadServiceTest extends UnitSuite with UnitTestEnvironment{

  var service: ReadService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_OWNER = "published_owner"
  val PRIVATE_OWNER = "private_owner"

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), Some(1), None, List(), List(), None, Some(1), LearningPathStatus.PUBLISHED, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PUBLISHED_OWNER)
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), Some(1), None, List(), List(), None, Some(1), LearningPathStatus.PRIVATE, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PRIVATE_OWNER)

  val STEP1 = LearningStep(Some(1), Some(1), None, None, 1, List(), List(), List(), StepType.TEXT, None, true)
  val STEP2 = LearningStep(Some(2), Some(1), None, None, 2, List(), List(), List(), StepType.TEXT, None, false)

  override def beforeEach() = {
    service = new ReadService
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

  test("That withId returns a learningPath when the status is PUBLISHED and user is not the owner") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))

    val learningPath = service.withId(PUBLISHED_ID, Some(PRIVATE_OWNER))
    assert(learningPath.isDefined)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.withId(PRIVATE_ID) }.getMessage
    }
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.withId(PRIVATE_ID, Some(PUBLISHED_OWNER)) }.getMessage
    }
  }

  test("That withId returns a learningPath when the status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    val learningPath = service.withId(PRIVATE_ID, Some(PRIVATE_OWNER))
    assert(learningPath.isDefined)
    assert(learningPath.get.id == PRIVATE_ID)
    assert(learningPath.get.status == "PRIVATE")
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

  test("That statusFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException]{ service.statusFor(2) }.getMessage
    }
  }

  test("That statusFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException]{ service.statusFor(2, Some(PUBLISHED_OWNER)) }.getMessage
    }
  }

  test("That statusFor returns a LearningPathStatus when the status is PRIVATE and the user is the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("PRIVATE") {
      service.statusFor(PRIVATE_ID, Some(PRIVATE_OWNER)).map(_.status).get
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

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepsFor(PRIVATE_ID) }.getMessage
    }
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepsFor(PRIVATE_ID, Some(PUBLISHED_OWNER)) }.getMessage
    }
  }

  test("That learningstepsFor returns all learningsteps for a learningpath when the status is PRIVATE and the user is the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PRIVATE_ID)).thenReturn(List(STEP1, STEP2))
    assertResult(2) {
      service.learningstepsFor(PRIVATE_ID, Some(PRIVATE_OWNER)).get.length
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

  test("That learningstepFor returns the LearningStep when it exists and status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service.learningstepFor(PRIVATE_ID, STEP1.id.get, Some(PRIVATE_OWNER)).get.id
    }
  }

  test("That learningstepFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepFor(PRIVATE_ID, STEP1.id.get) }.getMessage
    }
  }

  test("That learningstepFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepFor(PRIVATE_ID, STEP1.id.get, Some(PUBLISHED_OWNER)) }.getMessage
    }
  }

}
