package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi.{NewLearningStep, LearningPathStatus, NewLearningPath, UnitSpec}
import no.ndla.learningpathapi.business.LearningpathData
import no.ndla.learningpathapi.model.{ValidationException, AccessDeniedException, LearningStep, LearningPath}
import org.mockito.Matchers._
import org.mockito.Mockito._

class UpdateServiceTest extends UnitSpec {

  var learningPathDataMock: LearningpathData = _
  var updateService: UpdateService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID = 2

  val PUBLISHED_OWNER = "eier1"
  val PRIVATE_OWNER = "eier2"

  val PUBLISHED_LEARNINGPATH = LearningPath(Some(PUBLISHED_ID), List(), List(), None, 1, "PUBLISHED", "", new Date(), List(), PUBLISHED_OWNER)
  val PRIVATE_LEARNINGPATH = LearningPath(Some(PRIVATE_ID), List(), List(), None, 1, "PRIVATE", "", new Date(), List(), PRIVATE_OWNER)
  val NEW_PRIVATE_LEARNINGPATH = NewLearningPath(List(), List(), None, 1, List())
  val NEW_PUBLISHED_LEARNINGPATH = NewLearningPath(List(), List(), None, 1, List())

  val STEP1 = LearningStep(Some(1), None, 1, List(), List(), List(), "", None)
  val STEP2 = LearningStep(Some(2), None, 2, List(), List(), List(), "", None)
  val NEW_STEP: NewLearningStep = NewLearningStep(List(), List(), List(), "", None)

  override def beforeEach() = {
    learningPathDataMock = mock[LearningpathData]
    updateService = new UpdateService(learningPathDataMock)
  }


  "UpdateService.addLearningPath" should "insert the given LearningPath" in {
    when(learningPathDataMock.insert(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)
    val saved = updateService.addLearningPath(NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    assert(saved.id == PRIVATE_LEARNINGPATH.id.get)

    verify(learningPathDataMock, times(1)).insert(any[LearningPath])
  }

  "UpdateService.updateLearningPath" should "return None when the given ID does not exist" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None){
      updateService.updateLearningPath(PRIVATE_ID, NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    }
  }

  it should "update the learningpath when the given user is the owner if the status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(PRIVATE_LEARNINGPATH.id.get){
      updateService.updateLearningPath(PRIVATE_ID, NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).update(any[LearningPath])

  }

  it should "update the learningpath when the given user is the owner if the status is PUBLISHED" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(PUBLISHED_LEARNINGPATH.id.get){
      updateService.updateLearningPath(PUBLISHED_ID, NEW_PUBLISHED_LEARNINGPATH, PUBLISHED_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.updateLearningPath(PUBLISHED_ID, NEW_PUBLISHED_LEARNINGPATH, PRIVATE_OWNER) }.getMessage
    }
  }

  "UpdateService.updateLearningPathStatus" should "return None when the given ID does not exist" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None){
      updateService.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("PUBLISHED"), PRIVATE_OWNER)
    }
  }

  it should "throw a ValidationException if the status is not valid" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult("'Invalid' is not a valid publishingstatus.") {
      intercept[ValidationException] { updateService.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("Invalid"), PRIVATE_OWNER) }.getMessage
    }
  }

  it should "update the status when the given user is the owner and the status is PUBLISHED" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH.copy(status = "PRIVATE"))
    assertResult("PRIVATE"){
      updateService.updateLearningPathStatus(PUBLISHED_ID, LearningPathStatus("PRIVATE"), PUBLISHED_OWNER).get.status
    }
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "update the status when the given user is the owner and the status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH.copy(status = "PUBLISHED"))
    assertResult("PUBLISHED"){
      updateService.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("PUBLISHED"), PRIVATE_OWNER).get.status
    }
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.updateLearningPathStatus(PUBLISHED_ID, LearningPathStatus("PRIVATE"), PRIVATE_OWNER) }.getMessage
    }
  }

  "UpdateService.deleteLearningPath" should "return false when the given ID does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(false) {
      updateService.deleteLearningPath(PUBLISHED_ID, PUBLISHED_OWNER)
    }
  }

  it should "delete the learningpath when the given user is the owner. Regardless of status" in {
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
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.deleteLearningPath(PRIVATE_ID, PUBLISHED_OWNER) }.getMessage
    }
  }

  "UpdateService.addLearningStep" should "return None when the given learningpath does not exist" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None) {
      updateService.addLearningStep(PRIVATE_ID, NEW_STEP, PRIVATE_OWNER)
    }
    verify(learningPathDataMock, never()).insertLearningStep(any[LearningStep])
    verify(learningPathDataMock, never()).update(any[LearningPath])
  }

  it should "insert the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.insertLearningStep(any[LearningStep])).thenReturn(STEP1)
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(STEP1.id.get){
      updateService.addLearningStep(PRIVATE_ID, NEW_STEP, PRIVATE_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).insertLearningStep(any[LearningStep])
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "insert the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.insertLearningStep(any[LearningStep])).thenReturn(STEP2)
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP2.id.get){
      updateService.addLearningStep(PUBLISHED_ID, NEW_STEP, PUBLISHED_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).insertLearningStep(any[LearningStep])
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.addLearningStep(PUBLISHED_ID, NEW_STEP, PRIVATE_OWNER) }.getMessage
    }
  }

  "UpdateService.updateLearningStep" should "return None when the learningpath does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(None){
      updateService.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, never).updateLearningStep(any[LearningStep])
    verify(learningPathDataMock, never).update(any[LearningPath])
  }

  it should "return None when the learningstep does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)
    assertResult(None){
      updateService.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, never).updateLearningStep(any[LearningStep])
    verify(learningPathDataMock, never).update(any[LearningPath])
  }

  it should "update the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    when(learningPathDataMock.updateLearningStep(any[LearningStep])).thenReturn(STEP1)
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP1.id.get){
      updateService.updateLearningStep(PUBLISHED_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).updateLearningStep(any[LearningStep])
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "update the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    when(learningPathDataMock.updateLearningStep(any[LearningStep])).thenReturn(STEP1)
    when(learningPathDataMock.update(any[LearningPath])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(STEP1.id.get){
      updateService.updateLearningStep(PRIVATE_ID, STEP1.id.get, NEW_STEP, PRIVATE_OWNER).get.id
    }
    verify(learningPathDataMock, times(1)).updateLearningStep(any[LearningStep])
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.updateLearningStep(PRIVATE_ID, STEP1.id.get, NEW_STEP, PUBLISHED_OWNER) }.getMessage
    }
  }

  "UpdateService.deleteLearningStep" should "return false when the given learningpath does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(None)
    assertResult(false){
      updateService.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, never).deleteLearningStep(STEP1.id.get)
  }

  it should "return false when the given learningstep does not exist" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)
    assertResult(false){
      updateService.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, never).deleteLearningStep(STEP1.id.get)
  }

  it should "delete the learningstep when the given user is the owner and the status is PRIVATE" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult(true) {
      updateService.deleteLearningStep(PRIVATE_ID, STEP1.id.get, PRIVATE_OWNER)
    }
    verify(learningPathDataMock, times(1)).deleteLearningStep(STEP1.id.get)
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "delete the learningstep when the given user is the owner and the status is PUBLISHED" in {
    when(learningPathDataMock.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult(true) {
      updateService.deleteLearningStep(PUBLISHED_ID, STEP1.id.get, PUBLISHED_OWNER)
    }
    verify(learningPathDataMock, times(1)).deleteLearningStep(STEP1.id.get)
    verify(learningPathDataMock, times(1)).update(any[LearningPath])
  }

  it should "throw an AccessDeniedException when the given user is NOT the owner" in {
    when(learningPathDataMock.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathDataMock.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { updateService.deleteLearningStep(PRIVATE_ID, STEP1.id.get, PUBLISHED_OWNER) }.getMessage
    }
  }
}
