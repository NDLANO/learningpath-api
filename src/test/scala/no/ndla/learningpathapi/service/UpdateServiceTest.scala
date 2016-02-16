package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.LearningPathStatus
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.business.{LearningPathIndex, LearningpathData}
import no.ndla.learningpathapi.model.{ValidationException, AccessDeniedException, LearningStep, LearningPath, StepType}
import org.mockito.Matchers._
import org.mockito.Mockito._

class UpdateServiceTest extends UnitSuite {

  var learningPathDataMock: LearningpathData = _
  var searchIndexMock: LearningPathIndex = _
  var updateService: UpdateService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_OWNER = "eier1"
  val PRIVATE_OWNER = "eier2"

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), None, List(), List(), None, 1, model.LearningPathStatus.PUBLISHED, model.LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PUBLISHED_OWNER)
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), None, List(), List(), None, 1, model.LearningPathStatus.PRIVATE, model.LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PRIVATE_OWNER)
  val NEW_PRIVATE_LEARNINGPATH = NewLearningPath(List(), List(), None, 1, List())
  val NEW_PUBLISHED_LEARNINGPATH = NewLearningPath(List(), List(), None, 1, List())

  val STEP1 = LearningStep(Some(1), None, None, 1, List(), List(), List(), StepType.TEXT, None)
  val STEP2 = LearningStep(Some(2), None, None, 2, List(), List(), List(), StepType.TEXT, None)
  val NEW_STEP: NewLearningStep = NewLearningStep(List(), List(), List(), "", None)

  override def beforeEach() = {
    learningPathDataMock = mock[LearningpathData]
    searchIndexMock = mock[LearningPathIndex]
    updateService = new UpdateService(learningPathDataMock, searchIndexMock)
  }

  test("That addLearningPath inserts the given LearningPath") {
    when(learningPathDataMock.insert(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)
    val saved = updateService.addLearningPath(NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    assert(saved.id == PRIVATE_LEARNINGPATH.id.get)

    verify(learningPathDataMock, times(1)).insert(any[LearningPath])
    verify(searchIndexMock, never).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningPath returns None when the given ID does not exist") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None){
      updateService.updateLearningPath(PRIVATE_ID, NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    }
  }

  test("That updateLearningPath updates the learningpath when the given user is the owner if the status is PRIVATE") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(PRIVATE_LEARNINGPATH.id.get){
      updateService.updateLearningPath(PRIVATE_ID, NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, never).indexLearningPath(any[LearningPath])

  }

  test("That updateLearningPath updates the learningpath when the given user is the owner if the status is PUBLISHED") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(PUBLISHED_LEARNINGPATH.id.get){
      updateService.updateLearningPath(PUBLISHED_ID, NEW_PUBLISHED_LEARNINGPATH, PUBLISHED_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningPath throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.updateLearningPath(PUBLISHED_ID, NEW_PUBLISHED_LEARNINGPATH, PRIVATE_OWNER) }.getMessage
    }
  }

  test("That updateLearningPathStatus returns None when the given ID does not exist") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None){
      updateService.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("PUBLISHED"), PRIVATE_OWNER)
    }
  }

  test("That updateLearningPathStatus throws a ValidationException if the status is not valid") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult("'Invalid' is not a valid publishingstatus.") {
      intercept[ValidationException] { updateService.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("Invalid"), PRIVATE_OWNER) }.getMessage
    }
  }

  test("That updateLearningPathStatus updates the status when the given user is the owner and the status is PUBLISHED") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH.copy(status = model.LearningPathStatus.PRIVATE))
    assertResult("PRIVATE"){
      updateService.updateLearningPathStatus(PUBLISHED_ID, LearningPathStatus("PRIVATE"), PUBLISHED_OWNER).get.status
    }
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, times(1)).deleteLearningPath(any[LearningPath])
  }

  test("That updateLearningPathStatus updates the status when the given user is the owner and the status is PRIVATE") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH.copy(status = model.LearningPathStatus.PUBLISHED))
    assertResult("PUBLISHED"){
      updateService.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("PUBLISHED"), PRIVATE_OWNER).get.status
    }
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningPathStatus throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.updateLearningPathStatus(PUBLISHED_ID, LearningPathStatus("PRIVATE"), PRIVATE_OWNER) }.getMessage
    }
  }

  test("That deleteLearningPath returns false when the given ID does not exist") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(false) {
      updateService.deleteLearningPath(PUBLISHED_ID, PUBLISHED_OWNER)
    }
  }

  test("That deleteLearningPath deletes the learningpath when the given user is the owner. Regardless of status") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult(true) {
      updateService.deleteLearningPath(PUBLISHED_ID, PUBLISHED_OWNER)
    }
    assertResult(true) {
      updateService.deleteLearningPath(PRIVATE_ID, PRIVATE_OWNER)
    }

    verify(learningPathDataMock, times(1)).delete(PUBLISHED_ID)
    verify(learningPathDataMock, times(1)).delete(PRIVATE_ID)
    verify(searchIndexMock, times(1)).deleteLearningPath(PUBLISHED_LEARNINGPATH)
  }

  test("That deleteLearningPath throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.deleteLearningPath(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That addLearningStep returns None when the given learningpath does not exist") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      updateService.addLearningStep(PRIVATE_ID, NEW_STEP, PRIVATE_OWNER)
    }
    verify(learningPathDataMock, never()).insertLearningStep(any[LearningStep])
    verify(learningPathDataMock, never()).update(any[LearningPath])
  }

  test("That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.insertLearningStep(any[LearningStep])).thenReturn(STEP1)
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(STEP1.id.get){
      updateService.addLearningStep(PRIVATE_ID, NEW_STEP, PRIVATE_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).insertLearningStep(any[LearningStep])
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, never).indexLearningPath(any[LearningPath])
  }

  test("That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.insertLearningStep(any[LearningStep])).thenReturn(STEP2)
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP2.id.get){
      updateService.addLearningStep(PUBLISHED_ID, NEW_STEP, PUBLISHED_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).insertLearningStep(any[LearningStep])
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That addLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.addLearningStep(PUBLISHED_ID, NEW_STEP, PRIVATE_OWNER) }.getMessage
    }
  }

  test("That updateLearningStep returns None when the learningpath does not exist") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None){
      updateService.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, never).updateLearningStep(any[LearningStep])
    verify(learningPathDataMock, never).update(any[LearningPath])
  }

  test("That updateLearningStep returns None when the learningstep does not exist") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)
    assertResult(None){
      updateService.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, never).updateLearningStep(any[LearningStep])
    verify(learningPathDataMock, never).update(any[LearningPath])
  }

  test("That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    when(learningPathDataMock.updateLearningStep(any[LearningStep])).thenReturn(STEP1)
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP1.id.get){
      updateService.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).updateLearningStep(any[LearningStep])
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    when(learningPathDataMock.updateLearningStep(any[LearningStep])).thenReturn(STEP1)
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(STEP1.id.get){
      updateService.updateLearningStep(PRIVATE_ID, STEP1.id.get, NEW_STEP, PRIVATE_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).updateLearningStep(any[LearningStep])
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, never).indexLearningPath(any[LearningPath])
  }

  test("That updateLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.updateLearningStep(PRIVATE_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That deleteLearningStep returns false when the given learningpath does not exist") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(false){
      updateService.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, never).deleteLearningStep(STEP1.id.get)
  }

  test("That deleteLearningStep returns false when the given learningstep does not exist") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)
    assertResult(false){
      updateService.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, never).deleteLearningStep(STEP1.id.get)
  }

  test("That deleteLearningStep deletes the learningstep when the given user is the owner and the status is PRIVATE") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(true) {
      updateService.deleteLearningStep(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
    verify(learningPathDataMock, times(1)).deleteLearningStep(STEP1.id.get)
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, never).indexLearningPath(any[LearningPath])
  }

  test("That deleteLearningStep deletes the learningstep when the given user is the owner and the status is PUBLISHED") {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH)
    assertResult(true) {
      updateService.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, times(1)).deleteLearningStep(STEP1.id.get)
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
    verify(searchIndexMock, times(1)).indexLearningPath(any[LearningPath])
  }

  test("That deleteLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.deleteLearningStep(PRIVATE_ID, STEP1.id.get, PUBLISHED_OWNER) }.getMessage
    }
  }
}
