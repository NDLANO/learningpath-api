package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.api.{LearningPathStatus, NewLearningPath, NewLearningStep, UpdatedLearningPath, UpdatedLearningStep}
import no.ndla.learningpathapi.model.domain._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import scalikejdbc.DBSession

class UpdateServiceTest extends UnitSuite with UnitTestEnvironment {
  var service: UpdateService = _

  val PUBLISHED_ID: Long = 1
  val PRIVATE_ID: Long = 2

  val PUBLISHED_OWNER = "eier1"
  val PRIVATE_OWNER = "eier2"

  val STEP1 = domain.LearningStep(Some(1), Some(1), None, None, 0, List(), List(), List(), StepType.TEXT, None, showTitle = true, status = StepStatus.ACTIVE)
  val STEP2 = domain.LearningStep(Some(2), Some(1), None, None, 1, List(), List(), List(), StepType.TEXT, None, showTitle = false, status = StepStatus.ACTIVE)
  val STEP3 = domain.LearningStep(Some(3), Some(1), None, None, 2, List(), List(), List(), StepType.TEXT, None, showTitle = true, status = StepStatus.ACTIVE)
  val STEP4 = domain.LearningStep(Some(4), Some(1), None, None, 3, List(), List(), List(), StepType.TEXT, None, showTitle = false, status = StepStatus.ACTIVE)
  val STEP5 = domain.LearningStep(Some(5), Some(1), None, None, 4, List(), List(), List(), StepType.TEXT, None, showTitle = true, status = StepStatus.ACTIVE)
  val STEP6 = domain.LearningStep(Some(6), Some(1), None, None, 5, List(), List(), List(), StepType.TEXT, None, showTitle = false, status = StepStatus.ACTIVE)

  val NEW_STEP = NewLearningStep(List(api.Title("Tittel", Some("nb"))), List(api.Description("Beskrivelse", Some("nb"))), List(), true, "TEXT", None)
  val UPDATED_STEP = UpdatedLearningStep(1, List(api.Title("Tittel", Some("nb"))), List(api.Description("Beskrivelse", Some("nb"))), List(), Some(false), None, None)

  val rubio = Author("author", "Little Marco")
  val license = License("Public Domain", "Public Domain", None)
  val copyright = Copyright(license, "", List(rubio))
  val apiRubio = api.Author("author", "Little Marco")
  val apiLicense = api.License("Public Domain", "Public Domain", None)
  val apiCopyright = api.Copyright(apiLicense, "", List(apiRubio))

  val PUBLISHED_LEARNINGPATH = domain.LearningPath(Some(PUBLISHED_ID), Some(1), Some("1"), None, List(Title("Tittel", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), None, Some(1), domain.LearningPathStatus.PUBLISHED, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PUBLISHED_OWNER, copyright, STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil)
  val PUBLISHED_LEARNINGPATH_NO_STEPS = domain.LearningPath(Some(PUBLISHED_ID), Some(1), Some("1"), None, List(Title("Tittel", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), None, Some(1), domain.LearningPathStatus.PUBLISHED, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PUBLISHED_OWNER, copyright, List())
  val PRIVATE_LEARNINGPATH = domain.LearningPath(Some(PRIVATE_ID), Some(1), None, None, List(Title("Tittel", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), None, Some(1), domain.LearningPathStatus.PRIVATE, LearningPathVerificationStatus.EXTERNAL, new Date(), List(), PRIVATE_OWNER, copyright, STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil)
  val NEW_PRIVATE_LEARNINGPATH = NewLearningPath(List(api.Title("Tittel", Some("nb"))), List(api.Description("Beskrivelse", Some("nb"))), None, Some(1), List(), apiCopyright)
  val NEW_PUBLISHED_LEARNINGPATH = NewLearningPath(List(), List(), None, Some(1), List(), apiCopyright)

  val UPDATED_PRIVATE_LEARNINGPATH = UpdatedLearningPath(1, List(), List(), None, Some(1), List(), apiCopyright)
  val UPDATED_PUBLISHED_LEARNINGPATH = UpdatedLearningPath(1, List(), List(), None, Some(1), List(), apiCopyright)


  override def beforeEach() = {
    service = new UpdateService
    resetMocks()

    when(authClient.getUserName(any[String])).thenReturn(NdlaUserName(Some("fornavn"), Some("mellomnavn"), Some("Etternavn")))
  }

  test("That addLearningPath inserts the given LearningPath") {
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    val saved = service.addLearningPath(NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    assert(saved.id == PRIVATE_LEARNINGPATH.id.get)

    verify(learningPathRepository, times(1)).insert(any[domain.LearningPath])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That updateLearningPath returns None when the given ID does not exist") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(None)
    assertResult(None){
      service.updateLearningPath(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    }
  }

  test("That updateLearningPath updates the learningpath when the given user is the owner if the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(PRIVATE_LEARNINGPATH.id.get){
      service.updateLearningPath(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATH, PRIVATE_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])

  }

  test("That updateLearningPath updates the learningpath when the given user is the owner if the status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(PUBLISHED_LEARNINGPATH.id.get){
      service.updateLearningPath(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATH, PUBLISHED_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(1)).indexLearningPath(any[domain.LearningPath])
  }

  test("That updateLearningPath throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.updateLearningPath(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATH, PRIVATE_OWNER) }.getMessage
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
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH.copy(status = domain.LearningPathStatus.PRIVATE))
    assertResult("PRIVATE"){
      service.updateLearningPathStatus(PUBLISHED_ID, LearningPathStatus("PRIVATE"), PUBLISHED_OWNER).get.status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(1)).deleteLearningPath(any[domain.LearningPath])
  }

  test("That updateLearningPathStatus updates the status when the given user is the owner and the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH.copy(status = domain.LearningPathStatus.PUBLISHED))
    assertResult("PUBLISHED"){
      service.updateLearningPathStatus(PRIVATE_ID, LearningPathStatus("PUBLISHED"), PRIVATE_OWNER).get.status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(1)).indexLearningPath(any[domain.LearningPath])
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
    verify(learningPathRepository, never()).insertLearningStep(any[domain.LearningStep])
    verify(learningPathRepository, never()).update(any[domain.LearningPath])
  }

  test("That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(STEP1.id.get){
      service.addLearningStep(PRIVATE_ID, NEW_STEP, PRIVATE_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).insertLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP2)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP2.id.get){
      service.addLearningStep(PUBLISHED_ID, NEW_STEP, PUBLISHED_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).insertLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexLearningPath(any[domain.LearningPath])
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
      service.updateLearningStep(PUBLISHED_ID, STEP1.id.get, UPDATED_STEP, PUBLISHED_OWNER)
    }
    verify(learningPathRepository, never).updateLearningStep(any[domain.LearningStep])
    verify(learningPathRepository, never).update(any[domain.LearningPath])
  }

  test("That updateLearningStep returns None when the learningstep does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(None)
    assertResult(None){
      service.updateLearningStep(PUBLISHED_ID, STEP1.id.get, UPDATED_STEP, PUBLISHED_OWNER)
    }
    verify(learningPathRepository, never).updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, never).update(any[domain.LearningPath])(any[DBSession])
  }

  test("That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP1.id.get){
      service.updateLearningStep(PUBLISHED_ID, STEP1.id.get, UPDATED_STEP, PUBLISHED_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexLearningPath(any[domain.LearningPath])
  }

  test("That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    assertResult(STEP1.id.get){
      service.updateLearningStep(PRIVATE_ID, STEP1.id.get, UPDATED_STEP, PRIVATE_OWNER).get.id
    }
    verify(learningPathRepository, times(1)).updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That updateLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] { service.updateLearningStep(PRIVATE_ID, STEP1.id.get, UPDATED_STEP, PUBLISHED_OWNER) }.getMessage
    }
  }

  test("That updateLearningStepStatus returns None when the given learningpath does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)

    service.updateLearningStepStatus(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER) should be (None)
  }

  test("That updateLearningStepStatus returns None when the given learningstep does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PUBLISHED_ID, STEP1.id.get)).thenReturn(None)

    service.updateLearningStepStatus(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER) should be (None)
  }

  test("That updateLearningStepStatus marks the learningstep as DELETED when the given user is the owner and the status is PRIVATE") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])).thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    val updatedStep = service.updateLearningStepStatus(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isDefined should be (true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That updateLearningStepStatus marks the learningstep as DELETED when the given user is the owner and the status is PUBLISHED") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])).thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    val updatedStep = service.updateLearningStepStatus(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    updatedStep.isDefined should be (true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexLearningPath(any[domain.LearningPath])
  }

  test("That marking the first learningStep as deleted changes the seqNo for all other learningsteps") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])).thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    val updatedStep = service.updateLearningStepStatus(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isDefined should be (true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That marking the first learningStep as active changes the seqNo for all other learningsteps") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.ACTIVE)))(any[DBSession])).thenReturn(STEP1.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    val updatedStep = service.updateLearningStepStatus(PRIVATE_ID, STEP1.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isDefined should be (true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.toString)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP1.copy(status = StepStatus.ACTIVE)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That marking the last learningStep as deleted does not affect any of the other learningsteps") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP3.id.get))(any[DBSession])).thenReturn(Some(STEP3))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP3.copy(status = StepStatus.DELETED)))(any[DBSession])).thenReturn(STEP3.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    val updatedStep = service.updateLearningStepStatus(PRIVATE_ID, STEP3.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isDefined should be (true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1)).updateLearningStep(any[LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That marking the last learningStep as active does not affect any of the other learningsteps") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP3.id.get))(any[DBSession])).thenReturn(Some(STEP3.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP3.copy(status = StepStatus.ACTIVE)))(any[DBSession])).thenReturn(STEP3.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    val updatedStep = service.updateLearningStepStatus(PRIVATE_ID, STEP3.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isDefined should be (true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.toString)

    verify(learningPathRepository, times(1)).updateLearningStep(any[LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That marking the middle learningStep as deleted only affects subsequen learningsteps") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession])).thenReturn(Some(STEP2))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP2.copy(status = StepStatus.DELETED)))(any[DBSession])).thenReturn(STEP2.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    val updatedStep = service.updateLearningStepStatus(PRIVATE_ID, STEP2.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isDefined should be (true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP2.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That marking the middle learningStep as active only affects subsequen learningsteps") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession])).thenReturn(Some(STEP2.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP2.copy(status = StepStatus.ACTIVE)))(any[DBSession])).thenReturn(STEP2.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    val updatedStep = service.updateLearningStepStatus(PRIVATE_ID, STEP2.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isDefined should be (true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.toString)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP2.copy(status = StepStatus.ACTIVE)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexLearningPath(any[domain.LearningPath])
  }

  test("That deleteLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    assertResult("You do not have access to the requested resource.") {
      intercept[AccessDeniedException] {service.updateLearningStepStatus(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)}.getMessage
    }
  }

  test("That updateSeqNo throws ValidationException when seqNo out of range") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))

    val exception = intercept[ValidationException] {
      service.updateSeqNo(PRIVATE_ID, STEP1.id.get, 100, PRIVATE_OWNER)
    }

    exception.errors.length should be (1)
    exception.errors.head.field should equal ("seqNo")
    exception.errors.head.message should equal ("seqNo must be between 0 and 5")
  }

  test("That updateSeqNo from 0 to last updates all learningsteps in between") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))

    val updatedStep = service.updateSeqNo(PRIVATE_ID, STEP1.id.get, STEP6.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal (STEP6.seqNo)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP4.copy(seqNo = STEP4.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP5.copy(seqNo = STEP5.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP6.copy(seqNo = STEP6.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP1.copy(seqNo = STEP6.seqNo)))(any[DBSession])
  }

  test("That updateSeqNo from last to 0 updates all learningsteps in between") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP6.id.get))(any[DBSession])).thenReturn(Some(STEP6))

    val updatedStep = service.updateSeqNo(PRIVATE_ID, STEP6.id.get, STEP1.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal (STEP1.seqNo)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP6.copy(seqNo = STEP1.seqNo)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP1.copy(seqNo = STEP1.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP4.copy(seqNo = STEP4.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP5.copy(seqNo = STEP5.seqNo + 1)))(any[DBSession])
  }

  test("That updateSeqNo between two middle steps only updates the two middle steps") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession])).thenReturn(Some(STEP2))

    val updatedStep = service.updateSeqNo(PRIVATE_ID, STEP2.id.get, STEP3.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal (STEP3.seqNo)

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP2.copy(seqNo = STEP3.seqNo)))(any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP3.copy(seqNo = STEP2.seqNo)))(any[DBSession])
  }

  test("That updateSeqNo also update seqNo for all affected steps") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])).thenReturn(Some(STEP1))

    val updatedStep = service.updateSeqNo(PRIVATE_ID, STEP1.id.get, 1, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal (1)

    verify(learningPathRepository, times(2)).updateLearningStep(any[LearningStep])(any[DBSession])
  }

  test("That newFromExisting throws exception when user is not owner of the path and the path is private") {
    when(learningPathRepository.withId(PRIVATE_ID)).thenReturn(Some(PRIVATE_LEARNINGPATH))

    assertResult("You do not have access to the requested resource."){
      intercept[AccessDeniedException] {
        service.newFromExisting(PRIVATE_ID, NEW_PRIVATE_LEARNINGPATH, PUBLISHED_OWNER)
      }.getMessage
    }
  }

  test("That newFromExisting returns None when given id does not exist") {
    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(None)
    service.newFromExisting(PUBLISHED_ID, NEW_PRIVATE_LEARNINGPATH, PUBLISHED_OWNER) should be (None)
  }

  test("That basic-information unique per learningpath is reset in newFromExisting") {
    val now = new Date()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH_NO_STEPS)

    service.newFromExisting(PUBLISHED_ID, NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)
    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER,
      lastUpdated = now
    )

    verify(learningPathRepository, times(1)).insert(eqTo(expectedNewLearningPath))
  }

  test("That all editable fields are overridden if specified in input in newFromExisting") {
    val now = new Date()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH_NO_STEPS)

    val titlesToOverride = List(api.Title("Overridden title", Some("nb")))
    val descriptionsToOverride = List(api.Description("Overridden description", Some("nb")))
    val tagsToOverride = List(api.LearningPathTags(Seq("Overridden tag"), Some("nb")))
    val coverPhotoToOverride = Some("http://api.ndla.no/images/someImage")
    val durationOverride = Some(100)

    service.newFromExisting(PUBLISHED_ID,
      NEW_PRIVATE_LEARNINGPATH.copy(
        title = titlesToOverride,
        description = descriptionsToOverride,
        tags = tagsToOverride,
        coverPhotoMetaUrl = coverPhotoToOverride,
        duration = durationOverride),
      PRIVATE_OWNER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER,
      lastUpdated = now,
      title = titlesToOverride.map(converterService.asTitle),
      description = descriptionsToOverride.map(converterService.asDescription),
      tags = tagsToOverride.map(converterService.asLearningPathTags),
      coverPhotoMetaUrl = coverPhotoToOverride,
      duration = durationOverride
    )

    verify(learningPathRepository, times(1)).insert(eqTo(expectedNewLearningPath))
  }

  test("That learningsteps are copied but with basic information reset in newFromExisting") {
    val now = new Date()
    when(clock.now()).thenReturn(now)

    val toCopy = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(learningsteps = STEP1 :: Nil)

    when(learningPathRepository.withId(PUBLISHED_ID)).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    service.newFromExisting(PUBLISHED_ID, NEW_PRIVATE_LEARNINGPATH, PRIVATE_OWNER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER,
      lastUpdated = now,
      learningsteps = PUBLISHED_LEARNINGPATH.learningsteps.map(_.copy(id = None, revision = None, externalId = None, learningPathId = None))
    )

    verify(learningPathRepository, times(1)).insert(eqTo(expectedNewLearningPath))

  }

  test("That mergeLanguageFields returns original list when updated is empty") {
    val existing = Seq(Title("Tittel 1", Some("nb")), Title("Tittel 2", Some("nn")), Title("Tittel 3", None))
    service.mergeLanguageFields(existing, Seq()) should equal (existing)
  }

  test("That mergeLanguageFields updated the english title only when specified") {
    val tittel1 = Title("Tittel 1", Some("nb"))
    val tittel2 = Title("Tittel 2", Some("nn"))
    val tittel3 = Title("Tittel 3", Some("en"))
    val oppdatertTittel3 = Title("Title 3 in english", Some("en"))

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel3)

    service.mergeLanguageFields(existing, updated) should equal (Seq(tittel1, tittel2, oppdatertTittel3))
  }

  test("That mergeLanguageFields removes a title that is empty") {
    val tittel1 = Title("Tittel 1", Some("nb"))
    val tittel2 = Title("Tittel 2", Some("nn"))
    val tittel3 = Title("Tittel 3", Some("en"))
    val tittelToRemove = Title("", Some("nn"))

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(tittelToRemove)

    service.mergeLanguageFields(existing, updated) should equal (Seq(tittel1, tittel3))
  }

  test("That mergeLanguageFields updates the title with no language specified") {
    val tittel1 = Title("Tittel 1", Some("nb"))
    val tittel2 = Title("Tittel 2", None)
    val tittel3 = Title("Tittel 3", Some("en"))
    val oppdatertTittel2 = Title("Tittel 2 er oppdatert", None)

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel2)

    service.mergeLanguageFields(existing, updated) should equal (Seq(tittel1, tittel3, oppdatertTittel2))
  }

  test("That mergeLanguageFields also updates the correct description") {
    val desc1 = Description("Beskrivelse 1", Some("nb"))
    val desc2 = Description("Beskrivelse 2", None)
    val desc3 = Description("Beskrivelse 3", Some("en"))
    val oppdatertDesc2 = Description("Beskrivelse 2 er oppdatert", None)

    val existing = Seq(desc1, desc2, desc3)
    val updated = Seq(oppdatertDesc2)

    service.mergeLanguageFields(existing, updated) should equal (Seq(desc1, desc3, oppdatertDesc2))
  }

}
