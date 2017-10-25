/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.api.{License}
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import no.ndla.learningpathapi.model._
import org.mockito.Mockito._
import org.mockito.Matchers._

class ReadServiceTest extends UnitSuite with UnitTestEnvironment{

  var service: ReadService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_OWNER = "published_owner"
  val PRIVATE_OWNER = "private_owner"
  val cruz = Author("author", "Lyin' Ted")
  val license = "publicdomain"
  val copyright = Copyright(license, List(cruz))

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), Some(1), None, None, List(Title("Tittel", "nb")), List(), None, Some(1), LearningPathStatus.PUBLISHED, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PUBLISHED_OWNER, copyright)
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), Some(1), None, None, List(Title("Tittel", "nb")), List(), None, Some(1), LearningPathStatus.PRIVATE, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PRIVATE_OWNER, copyright)

  val STEP1 = LearningStep(Some(1), Some(1), None, None, 1, List(Title("Tittel", "nb")), List(), List(), StepType.TEXT, None, showTitle = true, StepStatus.ACTIVE)
  val STEP2 = LearningStep(Some(2), Some(1), None, None, 2, List(Title("Tittel", "nb")), List(), List(), StepType.TEXT, None, showTitle = false, StepStatus.ACTIVE)
  val STEP3 = LearningStep(Some(3), Some(1), None, None, 3, List(Title("Tittel", "nb")), List(), List(), StepType.TEXT, None, showTitle = false, StepStatus.ACTIVE)

  override def beforeEach() = {
    service = new ReadService
    resetMocks()
  }

  test("That withIdV2 returns None when id does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      service.withIdV2(PUBLISHED_ID, "nb")
    }
  }

  test("That withIdV2 returns a learningPath when the status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val learningPath = service.withIdV2(PUBLISHED_ID, "nb")
    assert(learningPath.isDefined)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId returns a learningPath when the status is PUBLISHED and user is not the owner") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val learningPath = service.withIdV2(PUBLISHED_ID, "nb", Some(PRIVATE_OWNER))
    assert(learningPath.isDefined)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.withIdV2(PRIVATE_ID, "nb") }.getMessage
    }
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.withIdV2(PRIVATE_ID, "nb", Some(PUBLISHED_OWNER)) }.getMessage
    }
  }

  test("That withId returns a learningPath when the status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    val learningPath = service.withIdV2(PRIVATE_ID, "nb", Some(PRIVATE_OWNER))
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
      service.learningstepsForWithStatusV2(PUBLISHED_ID, StepStatus.ACTIVE, "nb")
    }
  }

  test("That learningstepsFor returns None the learningPath does not have any learningsteps") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PUBLISHED_ID)).thenReturn(List())
    assertResult(None) {
      service.learningstepsForWithStatusV2(PUBLISHED_ID, StepStatus.ACTIVE, "nb")
    }
  }

  test("That learningstepsFor returns only active steps when specifying status active") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PUBLISHED_ID)).thenReturn(List(STEP1, STEP2.copy(status = StepStatus.DELETED), STEP3))
    val learningSteps = service.learningstepsForWithStatusV2(PUBLISHED_ID, StepStatus.ACTIVE, "nb")
    learningSteps.isDefined should be (true)
    learningSteps.get.learningsteps.size should be (2)
    learningSteps.get.learningsteps.head.id should equal (STEP1.id.get)
    learningSteps.get.learningsteps.last.id should equal (STEP3.id.get)
  }

  test("That learningstepsFor returns only deleted steps when specifying status deleted") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PUBLISHED_ID)).thenReturn(List(STEP1, STEP2.copy(status = StepStatus.DELETED), STEP3))
    val learningSteps = service.learningstepsForWithStatusV2(PUBLISHED_ID, StepStatus.DELETED, "nb")
    learningSteps.isDefined should be (true)
    learningSteps.get.learningsteps.size should be (1)
    learningSteps.get.learningsteps.head.id should equal (STEP2.id.get)
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb") }.getMessage
    }
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", Some(PUBLISHED_OWNER)) }.getMessage
    }
  }

  test("That learningstepsFor returns all learningsteps for a learningpath when the status is PRIVATE and the user is the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(PRIVATE_ID)).thenReturn(List(STEP1, STEP2))
    assertResult(2) {
      service.learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", Some(PRIVATE_OWNER)).get.learningsteps.length
    }
  }

  test("That learningstepV2For returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None) {
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb")
    }
  }

  test("That learningstepV2For returns None when the learningStep does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)
    assertResult(None) {
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb")
    }
  }

  test("That learningstepV2For returns the LearningStep when it exists") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb").get.id
    }
  }

  test("That learningstepV2For returns the LearningStep when it exists and status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", Some(PRIVATE_OWNER)).get.id
    }
  }

  test("That learningstepV2For throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb") }.getMessage
    }
  }

  test("That learningstepFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", Some(PUBLISHED_OWNER)) }.getMessage
    }
  }

}
