/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.api.config.UpdateConfigValue
import no.ndla.learningpathapi.model.api.{
  NewCopyLearningPathV2,
  NewLearningPathV2,
  NewLearningStepV2,
  UpdatedLearningPathV2,
  UpdatedLearningStepV2
}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import scala.reflect.runtime.universe.Try
import scala.util.{Failure, Success}

class UpdateServiceTest extends UnitSuite with UnitTestEnvironment {
  var service: UpdateService = _

  val PUBLISHED_ID: Long = 1
  val PRIVATE_ID: Long = 2

  val PUBLISHED_OWNER = UserInfo("eier1", Set.empty)
  val PRIVATE_OWNER = UserInfo("eier2", Set.empty)

  val STEP1 = domain.LearningStep(Some(1),
                                  Some(1),
                                  None,
                                  None,
                                  0,
                                  List(domain.Title("Tittel", "nb")),
                                  List(),
                                  List(),
                                  StepType.TEXT,
                                  None,
                                  showTitle = true,
                                  status = StepStatus.ACTIVE)

  val STEP2 = domain.LearningStep(Some(2),
                                  Some(1),
                                  None,
                                  None,
                                  1,
                                  List(domain.Title("Tittel", "nb")),
                                  List(),
                                  List(),
                                  StepType.TEXT,
                                  None,
                                  showTitle = false,
                                  status = StepStatus.ACTIVE)

  val STEP3 = domain.LearningStep(Some(3),
                                  Some(1),
                                  None,
                                  None,
                                  2,
                                  List(domain.Title("Tittel", "nb")),
                                  List(),
                                  List(),
                                  StepType.TEXT,
                                  None,
                                  showTitle = true,
                                  status = StepStatus.ACTIVE)

  val STEP4 = domain.LearningStep(Some(4),
                                  Some(1),
                                  None,
                                  None,
                                  3,
                                  List(domain.Title("Tittel", "nb")),
                                  List(),
                                  List(),
                                  StepType.TEXT,
                                  None,
                                  showTitle = false,
                                  status = StepStatus.ACTIVE)

  val STEP5 = domain.LearningStep(Some(5),
                                  Some(1),
                                  None,
                                  None,
                                  4,
                                  List(domain.Title("Tittel", "nb")),
                                  List(),
                                  List(),
                                  StepType.TEXT,
                                  None,
                                  showTitle = true,
                                  status = StepStatus.ACTIVE)

  val STEP6 = domain.LearningStep(Some(6),
                                  Some(1),
                                  None,
                                  None,
                                  5,
                                  List(domain.Title("Tittel", "nb")),
                                  List(),
                                  List(),
                                  StepType.TEXT,
                                  None,
                                  showTitle = false,
                                  status = StepStatus.ACTIVE)

  val NEW_STEPV2 =
    NewLearningStepV2("Tittel", Some("Beskrivelse"), "nb", Some(api.EmbedUrlV2("", "oembed")), true, "TEXT", None)

  val UPDATED_STEPV2 =
    UpdatedLearningStepV2(1, Option("Tittel"), "nb", Some("Beskrivelse"), None, Some(false), None, None)

  val rubio = Author("author", "Little Marco")
  val license = "publicdomain"
  val copyright = Copyright(license, List(rubio))
  val apiRubio = api.Author("author", "Little Marco")
  val apiLicense = api.License("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
  val apiCopyright = api.Copyright(apiLicense, List(apiRubio))

  val PUBLISHED_LEARNINGPATH = domain.LearningPath(
    Some(PUBLISHED_ID),
    Some(1),
    Some("1"),
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.PUBLISHED,
    LearningPathVerificationStatus.EXTERNAL,
    new Date(),
    List(),
    PUBLISHED_OWNER.userId,
    copyright,
    Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil)
  )

  val PUBLISHED_LEARNINGPATH_NO_STEPS = domain.LearningPath(
    Some(PUBLISHED_ID),
    Some(1),
    Some("1"),
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.PUBLISHED,
    LearningPathVerificationStatus.EXTERNAL,
    new Date(),
    List(),
    PUBLISHED_OWNER.userId,
    copyright,
    None
  )

  val PRIVATE_LEARNINGPATH = domain.LearningPath(
    Some(PRIVATE_ID),
    Some(1),
    None,
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.EXTERNAL,
    new Date(),
    List(),
    PRIVATE_OWNER.userId,
    copyright,
    Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil)
  )

  val PRIVATE_LEARNINGPATH_NO_STEPS = domain.LearningPath(
    Some(PRIVATE_ID),
    Some(1),
    None,
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.EXTERNAL,
    new Date(),
    List(),
    PRIVATE_OWNER.userId,
    copyright,
    None
  )

  val DELETED_LEARNINGPATH = domain.LearningPath(
    Some(PRIVATE_ID),
    Some(1),
    None,
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.DELETED,
    LearningPathVerificationStatus.EXTERNAL,
    new Date(),
    List(),
    PRIVATE_OWNER.userId,
    copyright,
    Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil)
  )
  val NEW_PRIVATE_LEARNINGPATHV2 = NewLearningPathV2("Tittel", "Beskrivelse", None, Some(1), List(), "nb", apiCopyright)
  val NEW_COPIED_LEARNINGPATHV2 = NewCopyLearningPathV2("Tittel", Some("Beskrivelse"), "nb", None, Some(1), None, None)

  val UPDATED_PRIVATE_LEARNINGPATHV2 =
    UpdatedLearningPathV2(1, None, "nb", None, None, Some(1), None, Some(apiCopyright), None)

  val UPDATED_PUBLISHED_LEARNINGPATHV2 =
    UpdatedLearningPathV2(1, None, "nb", None, None, Some(1), None, Some(apiCopyright), None)

  override def beforeEach(): Unit = {
    service = new UpdateService
    resetMocks()
    when(readService.canWriteNow(any[UserInfo])).thenReturn(true)
    when(searchIndexService.deleteDocument(any[domain.LearningPath])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[domain.LearningPath](0)))
    when(searchIndexService.indexDocument(any[domain.LearningPath])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[domain.LearningPath](0)))
    when(taxononyApiClient.updateTaxonomyForLearningPath(any[domain.LearningPath], any[Boolean]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.LearningPath](0)))
    when(learningStepValidator.validate(any[LearningStep], any[Boolean])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[LearningStep](0)))
  }

  test("That addLearningPathV2 inserts the given LearningPathV2") {
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)

    val saved =
      service.addLearningPathV2(NEW_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER)
    assert(saved.get.id == PRIVATE_LEARNINGPATH.id.get)

    verify(learningPathRepository, times(1)).insert(any[domain.LearningPath])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathV2 returns Failure when the given ID does not exist") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER)
    ex should be(NotFoundException("Could not find learningpath with id '2'."))
  }

  test("That updateLearningPathV2 updates the learningpath when the given user is the owner if the status is PRIVATE") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(PRIVATE_LEARNINGPATH.id.get) {
      service
        .updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER)
        .get
        .id
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])

  }

  test("That updateLearningPathV2 updates the learningpath when the given user is the owner if the status is UNLISTED") {
    val unlistedLp = PRIVATE_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED)
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(unlistedLp))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(unlistedLp)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(PRIVATE_LEARNINGPATH.id.get) {
      service
        .updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER)
        .get
        .id
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningPathV2 updates the learningpath when the given user is a publisher if the status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(PUBLISHED_LEARNINGPATH.id.get) {
      service
        .updateLearningPathV2(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATHV2, PUBLISHED_OWNER)
        .get
        .id
    }
  }

  test("That updateLearningPathV2 returns Failure if user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) =
      service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, UserInfo("not_the_owner", Set.empty))
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningPathV2 sets status to UNLISTED if owner is not publisher and status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val result = service.updateLearningPathV2(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATHV2, PUBLISHED_OWNER).get
    result.id should be(PUBLISHED_LEARNINGPATH.id.get)
    result.status should be(LearningPathStatus.UNLISTED.toString)
  }

  test("That updateLearningPathV2 status PRIVATE remains PRIVATE if not publisher") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val result = service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER).get
    result.id should be(PRIVATE_LEARNINGPATH.id.get)
    result.status should be(LearningPathStatus.PRIVATE.toString)
  }

  test("That updateLearningPathStatusV2 returns None when the given ID does not exist") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.PUBLISHED, PRIVATE_OWNER, "nb")
    ex should be(NotFoundException(s"Could not find learningpath with id '$PRIVATE_ID'."))
  }

  test("That updateLearningPathStatusV2 updates the status when the given user is admin and the status is PUBLISHED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = domain.LearningPathStatus.PRIVATE))
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID)).thenReturn(List())

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID,
                                    LearningPathStatus.PRIVATE,
                                    PRIVATE_OWNER.copy(roles = Set(LearningPathRole.ADMIN)),
                                    "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(1))
      .deleteDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningPathStatusV2 updates the status when the given user is not the owner, but is admin and the status is PUBLISHED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = domain.LearningPathStatus.PRIVATE))
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID))
      .thenReturn(List())

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID,
                                    LearningPathStatus.PRIVATE,
                                    UserInfo("not_the_owner", Set(LearningPathRole.ADMIN)),
                                    "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(1))
      .deleteDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 updates the status when the given user is the owner and the status is PRIVATE") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH.copy(status = domain.LearningPathStatus.DELETED))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult("DELETED") {
      service
        .updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.DELETED, PRIVATE_OWNER, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 updates the status when the given user is owner and the status is DELETED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(DELETED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(DELETED_LEARNINGPATH.copy(status = domain.LearningPathStatus.UNLISTED))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult("UNLISTED") {
      service
        .updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.UNLISTED, PRIVATE_OWNER, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(0)).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 updates the status when the given user is publisher and the status is DELETED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(DELETED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(DELETED_LEARNINGPATH.copy(status = domain.LearningPathStatus.PUBLISHED))

    assertResult("PUBLISHED") {
      service
        .updateLearningPathStatusV2(PRIVATE_ID,
                                    LearningPathStatus.PUBLISHED,
                                    PRIVATE_OWNER.copy(roles = Set(LearningPathRole.ADMIN)),
                                    "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 updates isBasedOn when a PUBLISHED path is DELETED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = domain.LearningPathStatus.DELETED))
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID))
      .thenReturn(
        List(DELETED_LEARNINGPATH.copy(id = Some(9), isBasedOn = Some(PUBLISHED_ID)),
             DELETED_LEARNINGPATH.copy(id = Some(8), isBasedOn = Some(PUBLISHED_ID))))

    assertResult("DELETED") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID,
                                    LearningPathStatus.DELETED,
                                    PUBLISHED_OWNER.copy(roles = Set(LearningPathRole.ADMIN)),
                                    "nb")
        .get
        .status
    }

    verify(learningPathRepository, times(3)).update(any[domain.LearningPath])
    verify(learningPathRepository, times(1)).learningPathsWithIsBasedOn(any[Long])
    verify(searchIndexService, times(1))
      .deleteDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 throws an AccessDeniedException when non-admin tries to publish") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.PUBLISHED, PRIVATE_OWNER, "nb")
    ex should be(AccessDeniedException("You need to be a publisher to publish learningpaths."))
  }

  test("That updateLearningPathStatusV2 allows owner to edit PUBLISHED to PRIVATE") {
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.PRIVATE))

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.PRIVATE, PUBLISHED_OWNER, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).learningPathsWithIsBasedOn(any[Long])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(1)).deleteDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 allows owner to edit PUBLISHED to UNLISTED") {
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED))

    assertResult("UNLISTED") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.UNLISTED, PUBLISHED_OWNER, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).learningPathsWithIsBasedOn(any[Long])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])
    verify(searchIndexService, times(1)).deleteDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 throws an AccessDeniedException when non-owner tries to change status") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val Failure(ex) = service.updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.PRIVATE, PRIVATE_OWNER, "nb")
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningPathStatusV2 ignores message if not admin") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(clock.now()).thenReturn(new Date(0))

    val expected = PUBLISHED_LEARNINGPATH.copy(
      message = None,
      status = LearningPathStatus.PRIVATE,
      lastUpdated = clock.now()
    )

    service.updateLearningPathStatusV2(PUBLISHED_ID,
                                       LearningPathStatus.PRIVATE,
                                       PUBLISHED_OWNER,
                                       "nb",
                                       Some("new message"))
    verify(learningPathRepository, times(1)).update(expected)
  }

  test("That updateLearningPathStatusV2 adds message if admin") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(clock.now()).thenReturn(new Date(0))

    service.updateLearningPathStatusV2(PUBLISHED_ID,
                                       LearningPathStatus.PRIVATE,
                                       PRIVATE_OWNER.copy(roles = Set(LearningPathRole.ADMIN)),
                                       "nb",
                                       Some("new message"))
    verify(learningPathRepository, times(1)).update(
      PUBLISHED_LEARNINGPATH.copy(message = Some(Message("new message", PRIVATE_OWNER.userId, clock.now())),
                                  status = LearningPathStatus.PRIVATE,
                                  lastUpdated = clock.now())
    )
  }

  test("That addLearningStepV2 returns None when the given learningpath does not exist") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.addLearningStepV2(PRIVATE_ID, NEW_STEPV2, PRIVATE_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
    verify(learningPathRepository, never).insertLearningStep(any[domain.LearningStep])
    verify(learningPathRepository, never).update(any[domain.LearningPath])
  }

  test(
    "That addLearningStepV2 inserts the learningstepV2 and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(STEP1.id.get) {
      service.addLearningStepV2(PRIVATE_ID, NEW_STEPV2, PRIVATE_OWNER).get.id
    }
    verify(learningPathRepository, times(1))
      .insertLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
    verify(searchIndexService, times(1)).deleteDocument(any[domain.LearningPath])
  }

  test(
    "That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP2)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenAnswer((i: InvocationOnMock) => i.getArgument[domain.LearningPath](0))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    assertResult(STEP2.id.get) {
      service
        .addLearningStepV2(PUBLISHED_ID, NEW_STEPV2, PUBLISHED_OWNER)
        .get
        .id
    }
    verify(learningPathRepository, times(1))
      .insertLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(0)).indexDocument(any[domain.LearningPath])
    verify(searchIndexService, times(1)).deleteDocument(any[domain.LearningPath])
  }

  test("That addLearningStepV2 throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val Failure(ex) = service.addLearningStepV2(PUBLISHED_ID, NEW_STEPV2, PRIVATE_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningStepV2 returns None when the learningpathV2 does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)

    val Failure(ex) = service.updateLearningStepV2(PUBLISHED_ID, STEP1.id.get, UPDATED_STEPV2, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)

    verify(learningPathRepository, never).updateLearningStep(any[domain.LearningStep])
    verify(learningPathRepository, never).update(any[domain.LearningPath])
  }

  test("That updateLearningStepV2 returns None when the learningstep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(None)
    val Failure(ex) = service.updateLearningStepV2(PUBLISHED_ID, STEP1.id.get, UPDATED_STEPV2, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
    verify(learningPathRepository, never).updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, never).update(any[domain.LearningPath])(any[DBSession])
  }

  test(
    "That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is ADMIN and status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP1.id.get) {
      service
        .updateLearningStepV2(PUBLISHED_ID,
                              STEP1.id.get,
                              UPDATED_STEPV2,
                              PUBLISHED_OWNER.copy(roles = Set(LearningPathRole.ADMIN)))
        .get
        .id
    }
    verify(learningPathRepository, times(1))
      .updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningStepV2 updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(STEP1.id.get) {
      service
        .updateLearningStepV2(PRIVATE_ID, STEP1.id.get, UPDATED_STEPV2, PRIVATE_OWNER)
        .get
        .id
    }
    verify(learningPathRepository, times(1))
      .updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningStepV2 throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    val Failure(ex) = service.updateLearningStepV2(PRIVATE_ID, STEP1.id.get, UPDATED_STEPV2, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningStepStatusV2 returns None when the given learningpath does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)

    val Failure(ex) =
      service.updateLearningStepStatusV2(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test("That updateLearningStepStatusV2 returns None when the given learningstep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(None)

    val Failure(ex) =
      service.updateLearningStepStatusV2(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test(
    "That updateLearningStepStatusV2 marks the learningstep as DELETED when the given user is the owner and the status is PRIVATE") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningStepStatusV2 marks the learningstep as DELETED when the given user is the owner and the status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[domain.LearningPath](0))
    val updatedDate = new Date(0)
    when(clock.now()).thenReturn(updatedDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1)).update(
      eqTo(
        PUBLISHED_LEARNINGPATH.copy(
          learningsteps = PUBLISHED_LEARNINGPATH.learningsteps,
          status = LearningPathStatus.UNLISTED,
          lastUpdated = updatedDate
        )))(any[DBSession])
    verify(searchIndexService, times(0)).indexDocument(any[domain.LearningPath])
    verify(searchIndexService, times(1)).deleteDocument(any[domain.LearningPath])
  }

  test("That marking the first learningStep as deleted changes the seqNo for all other learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That marking the first learningStep as active changes the seqNo for all other learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(
      learningPathRepository
        .learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.ACTIVE)))(any[DBSession]))
      .thenReturn(STEP1.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.toString)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(status = StepStatus.ACTIVE)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That marking the last learningStep as deleted does not affect any of the other learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP3.id.get))(any[DBSession]))
      .thenReturn(Some(STEP3))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP3.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP3.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP3.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1))
      .updateLearningStep(any[LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That marking the last learningStep as active does not affect any of the other learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(
      learningPathRepository
        .learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP3.id.get))(any[DBSession]))
      .thenReturn(Some(STEP3.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP3.copy(status = StepStatus.ACTIVE)))(any[DBSession]))
      .thenReturn(STEP3.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP3.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.toString)

    verify(learningPathRepository, times(1))
      .updateLearningStep(any[LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That marking the middle learningStep as deleted only affects subsequent learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession]))
      .thenReturn(Some(STEP2))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP2.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP2.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP2.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.toString)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That marking the middle learningStep as active only affects subsequent learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(
      learningPathRepository
        .learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession]))
      .thenReturn(Some(STEP2.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP2.copy(status = StepStatus.ACTIVE)))(any[DBSession]))
      .thenReturn(STEP2.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP2.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.toString)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(status = StepStatus.ACTIVE)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That deleteLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get))
      .thenReturn(Some(STEP1))
    val Failure(ex) = service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateSeqNo throws ValidationException when seqNo out of range") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))

    val exception = intercept[ValidationException] {
      service.updateSeqNo(PRIVATE_ID, STEP1.id.get, 100, PRIVATE_OWNER)
    }

    exception.errors.length should be(1)
    exception.errors.head.field should equal("seqNo")
    exception.errors.head.message should equal("seqNo must be between 0 and 5")
  }

  test("That updateSeqNo from 0 to last updates all learningsteps in between") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))

    val updatedStep =
      service.updateSeqNo(PRIVATE_ID, STEP1.id.get, STEP6.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal(STEP6.seqNo)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP4.copy(seqNo = STEP4.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP5.copy(seqNo = STEP5.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP6.copy(seqNo = STEP6.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(seqNo = STEP6.seqNo)))(any[DBSession])
  }

  test("That updateSeqNo from last to 0 updates all learningsteps in between") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP6.id.get))(any[DBSession]))
      .thenReturn(Some(STEP6))

    val updatedStep =
      service.updateSeqNo(PRIVATE_ID, STEP6.id.get, STEP1.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal(STEP1.seqNo)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP6.copy(seqNo = STEP1.seqNo)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(seqNo = STEP1.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP4.copy(seqNo = STEP4.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP5.copy(seqNo = STEP5.seqNo + 1)))(any[DBSession])
  }

  test("That updateSeqNo between two middle steps only updates the two middle steps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession]))
      .thenReturn(Some(STEP2))

    val updatedStep =
      service.updateSeqNo(PRIVATE_ID, STEP2.id.get, STEP3.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal(STEP3.seqNo)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP3.seqNo)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP2.seqNo)))(any[DBSession])
  }

  test("That updateSeqNo also update seqNo for all affected steps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))

    val updatedStep =
      service.updateSeqNo(PRIVATE_ID, STEP1.id.get, 1, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal(1)

    verify(learningPathRepository, times(2))
      .updateLearningStep(any[LearningStep])(any[DBSession])
  }

  test("new fromExisting2 should allow laugage fields set to unknown") {
    val learningpathWithUnknownLang = PUBLISHED_LEARNINGPATH.copy(title = Seq(Title("what språk is this", "unknown")))

    when(learningPathRepository.withId(eqTo(learningpathWithUnknownLang.id.get))(any[DBSession]))
      .thenReturn(Some(learningpathWithUnknownLang))
    when(learningPathRepository.insert(any[LearningPath])(any[DBSession]))
      .thenReturn(learningpathWithUnknownLang)

    val newCopy =
      NewCopyLearningPathV2("hehe", None, "nb", None, None, None, None)
    service
      .newFromExistingV2(learningpathWithUnknownLang.id.get, newCopy, UserInfo("me", Set.empty))
      .isSuccess should be(true)
  }

  test("That newFromExistingV2 throws exception when user is not owner of the path and the path is private") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) = service.newFromExistingV2(PRIVATE_ID, NEW_COPIED_LEARNINGPATHV2, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("Owner updates step of published should update status to UNLISTED") {
    val newDate = new Date(648000000)
    val stepWithBadTitle = STEP1.copy(title = Seq(domain.Title("Dårlig tittel", "nb")))

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])).thenReturn(stepWithBadTitle)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[domain.LearningPath](0))
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedLs = UpdatedLearningStepV2(1, Some("Dårlig tittel"), "nb", None, None, None, None, None)
    service.updateLearningStepV2(PUBLISHED_ID, STEP1.id.get, updatedLs, PUBLISHED_OWNER)
    val updatedPath = PUBLISHED_LEARNINGPATH.copy(
      status = LearningPathStatus.UNLISTED,
      lastUpdated = newDate,
      learningsteps = Some(PUBLISHED_LEARNINGPATH.learningsteps.get.tail ++ List(stepWithBadTitle))
    )

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(updatedPath))(any[DBSession])
    verify(searchIndexService, times(0)).indexDocument(updatedPath)
    verify(searchIndexService, times(1)).deleteDocument(updatedPath)
  }

  test("owner updates published path should update status to unlisted") {
    val newDate = new Date(648000000)
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0))
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val lpToUpdate = UpdatedLearningPathV2(1, Some("YapThisUpdated"), "nb", None, None, None, None, None, None)
    service.updateLearningPathV2(PUBLISHED_ID, lpToUpdate, PUBLISHED_OWNER)

    val expectedUpdatedPath = PUBLISHED_LEARNINGPATH.copy(
      title = List(Title("YapThisUpdated", "nb")),
      status = LearningPathStatus.UNLISTED,
      lastUpdated = newDate
    )

    verify(learningPathRepository, times(1)).update(eqTo(expectedUpdatedPath))(any[DBSession])
    verify(searchIndexService, times(0)).indexDocument(expectedUpdatedPath)
    verify(searchIndexService, times(1)).deleteDocument(any[domain.LearningPath])
  }

  test("owner updates step private should not update status") {
    val newDate = new Date(648000000)
    val stepWithBadTitle = STEP1.copy(title = Seq(domain.Title("Dårlig tittel", "nb")))

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])).thenReturn(stepWithBadTitle)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0))
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedLs = UpdatedLearningStepV2(1, Some("Dårlig tittel"), "nb", None, None, None, None, None)
    service.updateLearningStepV2(PRIVATE_ID, STEP1.id.get, updatedLs, PRIVATE_OWNER)
    val updatedPath = PRIVATE_LEARNINGPATH.copy(
      lastUpdated = newDate,
      learningsteps = Some(PRIVATE_LEARNINGPATH.learningsteps.get.tail ++ List(stepWithBadTitle))
    )

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(updatedPath))(any[DBSession])
    verify(searchIndexService, times(0)).indexDocument(updatedPath)
    verify(searchIndexService, times(1)).deleteDocument(updatedPath)
  }

  test("admin updates step should not update status") {
    val newDate = new Date(648000000)
    val stepWithBadTitle = STEP1.copy(title = Seq(domain.Title("Dårlig tittel", "nb")))

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])).thenReturn(stepWithBadTitle)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0))
    when(clock.now()).thenReturn(newDate)

    val updatedLs = UpdatedLearningStepV2(1, Some("Dårlig tittel"), "nb", None, None, None, None, None)
    service.updateLearningStepV2(PUBLISHED_ID,
                                 STEP1.id.get,
                                 updatedLs,
                                 PUBLISHED_OWNER.copy(roles = Set(LearningPathRole.ADMIN)))
    val updatedPath = PUBLISHED_LEARNINGPATH.copy(
      lastUpdated = newDate,
      learningsteps = Some(PUBLISHED_LEARNINGPATH.learningsteps.get.tail ++ List(stepWithBadTitle))
    )

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(updatedPath))(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(updatedPath)
  }

  test("That newFromExistingV2 returns None when given id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test("That basic-information unique per learningpath is reset in newFromExistingV2") {
    val now = new Date()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH_NO_STEPS)

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.userId,
      lastUpdated = now
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))
  }

  test("That isBasedOn is not sat if the existing learningpath is PRIVATE") {
    val now = new Date()
    when(clock.now()).thenReturn(now)
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH_NO_STEPS)

    service.newFromExistingV2(PRIVATE_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER)

    val expectedNewLearningPath = PRIVATE_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = None,
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.userId,
      lastUpdated = now
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))
  }

  test("That isBasedOn is sat if the existing learningpath is PUBLISHED") {
    val now = new Date()
    when(clock.now()).thenReturn(now)
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH_NO_STEPS)

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.userId,
      lastUpdated = now
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))
  }

  test("That all editable fields are overridden if specified in input in newFromExisting") {
    val now = new Date()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH_NO_STEPS)

    val titlesToOverride = "Overridden title"
    val descriptionsToOverride = Some("Overridden description")
    val tagsToOverride = Some(Seq("Overridden tag"))
    val coverPhotoId = "9876"
    val coverPhotoToOverride = Some(s"http://api.ndla.no/images/$coverPhotoId")
    val durationOverride = Some(100)

    service.newFromExistingV2(
      PUBLISHED_ID,
      NEW_COPIED_LEARNINGPATHV2.copy(title = titlesToOverride,
                                     description = descriptionsToOverride,
                                     tags = tagsToOverride,
                                     coverPhotoMetaUrl = coverPhotoToOverride,
                                     duration = durationOverride),
      PRIVATE_OWNER
    )

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.userId,
      lastUpdated = now,
      title = Seq(converterService.asTitle(api.Title(titlesToOverride, "nb"))),
      description = descriptionsToOverride
        .map(desc => converterService.asDescription(api.Description(desc, "nb")))
        .toSeq,
      tags = tagsToOverride
        .map(tagSeq => converterService.asLearningPathTags(api.LearningPathTags(tagSeq, "nb")))
        .toSeq,
      coverPhotoId = Some(coverPhotoId),
      duration = durationOverride
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))
  }

  test("That learningsteps are copied but with basic information reset in newFromExistingV2") {
    val now = new Date()
    when(clock.now()).thenReturn(now)

    val toCopy =
      PUBLISHED_LEARNINGPATH_NO_STEPS.copy(learningsteps = Some(STEP1 :: Nil))

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH)

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.userId,
      lastUpdated = now,
      learningsteps = PUBLISHED_LEARNINGPATH.learningsteps.map(
        _.map(_.copy(id = None, revision = None, externalId = None, learningPathId = None)))
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))

  }

  test("That delete message field deletes admin message") {
    val newDate = new Date()
    val originalLearningPath =
      PUBLISHED_LEARNINGPATH.copy(message = Some(Message("You need to fix some stuffs", "kari", clock.now())))
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(originalLearningPath))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0))
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val lpToUpdate = UpdatedLearningPathV2(1, None, "nb", None, None, None, None, None, Some(true))
    service.updateLearningPathV2(PUBLISHED_ID, lpToUpdate, PUBLISHED_OWNER)

    val expectedUpdatedPath = PUBLISHED_LEARNINGPATH.copy(
      status = LearningPathStatus.UNLISTED,
      lastUpdated = newDate,
      message = None
    )

    verify(learningPathRepository, times(1)).update(eqTo(expectedUpdatedPath))(any[DBSession])
  }

  test("That writeOrAccessDenied denies writes while write restriction is enabled.") {
    val readMock = mock[ReadService]
    when(readService.canWriteNow(any[UserInfo])).thenReturn(false)

    service.writeDuringWriteRestrictionOrAccessDenied(UserInfo("SomeDude", roles = Set())) { Success(readMock.tags) }
    verify(readMock, times(0)).tags
  }

  test("That updating config returns failure for non-admin users") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(TestData.testConfigMeta))
    val Failure(ex) = service.updateConfig(ConfigKey.IsWriteRestricted,
                                           UpdateConfigValue("true"),
                                           UserInfo("Kari", Set(LearningPathRole.PUBLISH)))
    ex.isInstanceOf[AccessDeniedException] should be(true)
  }

  test("That updating config returns success if all is good") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(TestData.testConfigMeta))
    val Success(config) = service.updateConfig(ConfigKey.IsWriteRestricted,
                                               UpdateConfigValue("true"),
                                               UserInfo("Kari", Set(LearningPathRole.ADMIN)))
  }

  test("That validation fails if IsWriteRestricted is not a boolean") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(TestData.testConfigMeta))
    val Failure(ex) = service.updateConfig(ConfigKey.IsWriteRestricted,
                                           UpdateConfigValue("123"),
                                           UserInfo("Kari", Set(LearningPathRole.ADMIN)))

    ex.isInstanceOf[ValidationException] should be(true)
  }

  test("That validation succeeds if IsWriteRestricted is a boolean") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(TestData.testConfigMeta))
    val res = service.updateConfig(ConfigKey.IsWriteRestricted,
                                   UpdateConfigValue("true"),
                                   UserInfo("Kari", Set(LearningPathRole.ADMIN)))
    res.isSuccess should be(true)
  }
}
