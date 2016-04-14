package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.LearningPathStatus
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.{ValidationException, AccessDeniedException, LearningStep, LearningPath, StepType, NdlaUserName}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import scalikejdbc.DBSession

class UpdateServiceTest extends UnitSuite with TestEnvironment {
  
  var service: UpdateService = _

  val PUBLISHED_ID: Long = 1
  val PRIVATE_ID: Long = 2

  val PUBLISHED_OWNER = "eier1"
  val PRIVATE_OWNER = "eier2"

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), None, List(), List(), None, Some(1), model.LearningPathStatus.PUBLISHED, model.LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PUBLISHED_OWNER)
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), None, List(), List(), None, Some(1), model.LearningPathStatus.PRIVATE, model.LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PRIVATE_OWNER)
  val NEW_PRIVATE_LEARNINGPATH = NewLearningPath(List(), List(), None, Some(1), List())
  val NEW_PUBLISHED_LEARNINGPATH = NewLearningPath(List(), List(), None, Some(1), List())

  val STEP1 = LearningStep(Some(1), None, None, 1, List(), List(), List(), StepType.TEXT, None)
  val STEP2 = LearningStep(Some(2), None, None, 2, List(), List(), List(), StepType.TEXT, None)
  val NEW_STEP: NewLearningStep = NewLearningStep(List(), List(), List(), "", None)

  override def beforeEach() = {
    service = new UpdateService
    resetMocks()

    when(authClient.getUserName(any[String])).thenReturn(NdlaUserName(Some("fornavn"), Some("mellomnavn"), Some("Etternavn")))
  }

  test("That addLearningPath inserts th e given LearningPath") {
    when(learningPathRepository.insert(any[LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    val saved = service.addLearningPath(NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    assert(saved.id == PRIVATE_LEARNINGPATH.id.get)

    verify(learningPathRepository, times(1)).insert(any[LearningPath])
    verify(searchIndexService, never).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningPath returns None when the given ID does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None){
      service.updateLearningPath(PRIVATE_ID, NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    }
  }

  test("That updateLearningPath updates the learningpath when the given user is the owner if the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(PRIVATE_LEARNINGPATH.id.get){
      service.updateLearningPath(PRIVATE_ID, NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, never).indexLearningPath(any[LearningPath])

  }

  test("That updateLearningPath updates the learningpath when the given user is the owner if the status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(PUBLISHED_LEARNINGPATH.id.get){
      service.updateLearningPath(PUBLISHED_ID, NEW_PUBLISHED_LEARNINGPATH, PUBLISHED_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningPath throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.updateLearningPath(PUBLISHED_ID, NEW_PUBLISHED_LEARNINGPATH, PRIVATE_OWNER) }.getMessage
    }
  }

  test("That updateLearningPathStatus returns None when the given ID does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None){
      service.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("PUBLISHED"), PRIVATE_OWNER)
    }
  }

  test("That updateLearningPathStatus updates the status when the given user is the owner and the status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH.copy(status = model.LearningPathStatus.PRIVATE))
    assertResult("PRIVATE"){
      service.updateLearningPathStatus(PUBLISHED_ID, LearningPathStatus("PRIVATE"), PUBLISHED_OWNER).get.status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, times(1)).deleteLearningPath(any[LearningPath])
  }

  test("That updateLearningPathStatus updates the status when the given user is the owner and the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH.copy(status = model.LearningPathStatus.PUBLISHED))
    assertResult("PUBLISHED"){
      service.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("PUBLISHED"), PRIVATE_OWNER).get.status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningPathStatus throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.updateLearningPathStatus(PUBLISHED_ID, LearningPathStatus("PRIVATE"), PRIVATE_OWNER) }.getMessage
    }
  }

  test("That deleteLearningPath returns false when the given ID does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(false) {
      service.deleteLearningPath(PUBLISHED_ID, PUBLISHED_OWNER)
    }
  }

  test("That deleteLearningPath deletes the learningpath when the given user is the owner. Regardless of status") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult(true) {
      service.deleteLearningPath(PUBLISHED_ID, PUBLISHED_OWNER)
    }
    assertResult(true) {
      service.deleteLearningPath(PRIVATE_ID, PRIVATE_OWNER)
    }

    verify(learningPathRepository, times(1)).delete(PUBLISHED_ID)
    verify(learningPathRepository, times(1)).delete(PRIVATE_ID)
    verify(searchIndexService, times(1)).deleteLearningPath(PUBLISHED_LEARNINGPATH)
  }

  test("That deleteLearningPath throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.deleteLearningPath(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That addLearningStep returns None when the given learningpath does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      service.addLearningStep(PRIVATE_ID, NEW_STEP, PRIVATE_OWNER)
    }
    verify(learningPathRepository, never()).insertLearningStep(any[LearningStep])
    verify(learningPathRepository, never()).update(any[LearningPath])
  }

  test("That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(STEP1.id.get){
      service.addLearningStep(PRIVATE_ID, NEW_STEP, PRIVATE_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).insertLearningStep(any[LearningStep])
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, never).indexLearningPath(any[LearningPath])
  }

  test("That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP2)
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP2.id.get){
      service.addLearningStep(PUBLISHED_ID, NEW_STEP, PUBLISHED_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).insertLearningStep(any[LearningStep])
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That addLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.addLearningStep(PUBLISHED_ID, NEW_STEP, PRIVATE_OWNER) }.getMessage
    }
  }

  test("That updateLearningStep returns None when the learningpath does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None){
      service.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER)
    }
    verify(learningPathRepository, never).updateLearningStep(any[LearningStep])
    verify(learningPathRepository, never).update(any[LearningPath])
  }

  test("That updateLearningStep returns None when the learningstep does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(None)
    assertResult(None){
      service.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER)
    }
    verify(learningPathRepository, never).updateLearningStep(any[LearningStep])
    verify(learningPathRepository, never).update(any[LearningPath])
  }

  test("That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP1.id.get){
      service.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).updateLearningStep(any[LearningStep])
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(STEP1.id.get){
      service.updateLearningStep(PRIVATE_ID, STEP1.id.get, NEW_STEP, PRIVATE_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).updateLearningStep(any[LearningStep])
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, never).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.updateLearningStep(PRIVATE_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That deleteLearningStep returns false when the given learningpath does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(false){
      service.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathRepository, never).deleteLearningStep(PUBLISHED_ID, STEP1.id.get)
  }

  test("That deleteLearningStep returns false when the given learningstep does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)
    assertResult(false){
      service.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathRepository, never).deleteLearningStep(PUBLISHED_ID, STEP1.id.get)
  }

  test("That deleteLearningStep deletes the learningstep when the given user is the owner and the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(true) {
      service.deleteLearningStep(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
    verify(learningPathRepository, times(1)).deleteLearningStep(PRIVATE_ID, STEP1.id.get)
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, never).indexLearningPath(any[LearningPath])
  }

  test("That deleteLearningStep deletes the learningstep when the given user is the owner and the status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)
    assertResult(true) {
      service.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathRepository, times(1)).deleteLearningStep(PUBLISHED_ID, STEP1.id.get)
    verify(learningPathRepository, times(1)).update(any[LearningPath])
    verify(searchIndexService, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That deleteLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.deleteLearningStep(PRIVATE_ID, STEP1.id.get, PUBLISHED_OWNER) }.getMessage
    }
  }
}
