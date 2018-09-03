/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.repository

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.domain._
import no.ndla.tag.IntegrationTest
import org.joda.time.DateTime
import scalikejdbc._

@IntegrationTest
class LearningPathRepositoryComponentIntegrationTest extends IntegrationSuite with TestEnvironment {
  var repository: LearningPathRepository = _

  val clinton = Author("author", "Hilla the Hun")
  val license = "publicdomain"
  val copyright = Copyright(license, List(clinton))

  val DefaultLearningPath = LearningPath(
    None,
    None,
    None,
    None,
    List(Title("UNIT-TEST-1", "unknown")),
    List(Description("UNIT-TEST", "unknown")),
    None,
    None,
    LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.EXTERNAL,
    new DateTime().withMillisOfSecond(0).toDate,
    List(),
    "UNIT-TEST",
    copyright
  )

  val DefaultLearningStep = LearningStep(
    None,
    None,
    None,
    None,
    0,
    List(Title("UNIT-TEST", "unknown")),
    List(Description("UNIT-TEST", "unknown")),
    List(EmbedUrl("http://www.vg.no", "unknown", EmbedType.OEmbed)),
    StepType.TEXT,
    None,
    true,
    StepStatus.ACTIVE
  )

  override def beforeEach(): Unit = {
    repository = new LearningPathRepository()
  }

  override def beforeAll(): Unit = {
    val datasource = getDataSource
    DBMigrator.migrate(datasource)
    ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
  }

  test("That insert, fetch and delete works happy-day") {
    inTransaction { implicit session =>
      val inserted = repository.insert(DefaultLearningPath)
      inserted.id.isDefined should be(true)

      val fetched = repository.withId(inserted.id.get)
      fetched.isDefined should be(true)
      fetched.get.id.get should equal(inserted.id.get)

      repository.deletePath(inserted.id.get)
    }
  }

  test("That transaction is rolled back if exception is thrown") {
    val owner = s"unit-test-${System.currentTimeMillis()}"
    deleteAllWithOwner(owner)

    try {
      inTransaction { implicit session =>
        val inserted =
          repository.insert(DefaultLearningPath.copy(owner = owner))
        throw new RuntimeException("Provoking exception inside transaction")
      }
      fail("Exception should prevent normal execution")
    } catch {
      case t: Throwable => {
        repository.withOwner(owner).length should be(0)
      }
    }
  }

  test("That updating several times is not throwing optimistic locking exception") {
    val inserted = repository.insert(DefaultLearningPath)
    val firstUpdate = repository.update(inserted.copy(title = List(Title("First change", "unknown"))))
    val secondUpdate = repository.update(firstUpdate.copy(title = List(Title("Second change", "unknown"))))
    val thirdUpdate = repository.update(secondUpdate.copy(title = List(Title("Third change", "unknown"))))

    inserted.revision should equal(Some(1))
    firstUpdate.revision should equal(Some(2))
    secondUpdate.revision should equal(Some(3))
    thirdUpdate.revision should equal(Some(4))

    repository.deletePath(thirdUpdate.id.get)
  }

  test("That trying to update a learningPath with old revision number throws optimistic locking exception") {
    val inserted = repository.insert(DefaultLearningPath)
    val firstUpdate = repository.update(inserted.copy(title = List(Title("First change", "unknown"))))

    assertResult(
      s"Conflicting revision is detected for learningPath with id = ${inserted.id} and revision = ${inserted.revision}") {
      intercept[OptimisticLockException] {
        repository.update(inserted.copy(title = List(Title("Second change, but with old revision", "unknown"))))
      }.getMessage
    }

    repository.deletePath(inserted.id.get)
  }

  test("That trying to update a learningStep with old revision throws optimistic locking exception") {
    val learningPath = repository.insert(DefaultLearningPath)
    val insertedStep = repository.insertLearningStep(DefaultLearningStep.copy(learningPathId = learningPath.id))
    val firstUpdate = repository.updateLearningStep(insertedStep.copy(title = List(Title("First change", "unknown"))))

    assertResult(
      s"Conflicting revision is detected for learningStep with id = ${insertedStep.id} and revision = ${insertedStep.revision}") {
      intercept[OptimisticLockException] {
        repository.updateLearningStep(insertedStep.copy(title = List(Title("First change", "unknown"))))
      }.getMessage
    }

    repository.deletePath(learningPath.id.get)
  }

  test("That learningPathsWithIsBasedOn returns all learningpaths that has one is based on id") {
    val learningPath1 = repository.insert(DefaultLearningPath)
    val learningPath2 = repository.insert(DefaultLearningPath)

    val copiedLearningPath1 = repository.insert(
      learningPath1.copy(
        id = None,
        isBasedOn = learningPath1.id
      ))
    val copiedLearningPath2 = repository.insert(
      learningPath1.copy(
        id = None,
        isBasedOn = learningPath1.id
      ))

    val learningPaths =
      repository.learningPathsWithIsBasedOn(learningPath1.id.get)

    learningPaths.map(_.id) should contain(copiedLearningPath1.id)
    learningPaths.map(_.id) should contain(copiedLearningPath2.id)
    learningPaths.map(_.id) should not contain (learningPath1.id)
    learningPaths.map(_.id) should not contain (learningPath2.id)
    learningPaths should have length (2)

    repository.deletePath(learningPath1.id.get)
    repository.deletePath(learningPath2.id.get)
    repository.deletePath(copiedLearningPath1.id.get)
    repository.deletePath(copiedLearningPath2.id.get)

  }

  test("That allPublishedTags returns only published tags") {
    val publicPath = repository.insert(
      DefaultLearningPath.copy(status = LearningPathStatus.PUBLISHED,
                               tags = List(LearningPathTags(Seq("aaa"), "nb"),
                                           LearningPathTags(Seq("bbb"), "nn"),
                                           LearningPathTags(Seq("ccc"), "en"))))

    val privatePath = repository.insert(DefaultLearningPath.copy(tags = List(LearningPathTags(Seq("ddd"), "nb"))))

    val publicTags = repository.allPublishedTags
    publicTags should contain(LearningPathTags(Seq("aaa"), "nb"))
    publicTags should contain(LearningPathTags(Seq("bbb"), "nn"))
    publicTags should contain(LearningPathTags(Seq("ccc"), "en"))
    publicTags should not contain LearningPathTags(Seq("ddd"), "nb")

    repository.deletePath(publicPath.id.get)
    repository.deletePath(privatePath.id.get)
  }

  test("That allPublishedTags removes duplicates") {
    val publicPath1 = repository.insert(
      DefaultLearningPath.copy(status = LearningPathStatus.PUBLISHED,
                               tags = List(LearningPathTags(Seq("aaa"), "nb"), LearningPathTags(Seq("aaa"), "nn"))))
    val publicPath2 = repository.insert(
      DefaultLearningPath.copy(status = LearningPathStatus.PUBLISHED,
                               tags = List(LearningPathTags(Seq("aaa", "bbb"), "nb"))))

    val publicTags = repository.allPublishedTags
    publicTags should contain(LearningPathTags(Seq("aaa", "bbb"), "nb"))
    publicTags should contain(LearningPathTags(Seq("aaa"), "nn"))

    publicTags
      .find(_.language.contains("nb"))
      .map(_.tags.count(_ == "aaa"))
      .getOrElse(0) should be(1)

    repository.deletePath(publicPath1.id.get)
    repository.deletePath(publicPath2.id.get)
  }

  test("That allPublishedContributors returns only published contributors") {
    val publicPath = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        copyright = Copyright("by",
                              List(Author("forfatter", "James Bond"),
                                   Author("forfatter", "Christian Bond"),
                                   Author("forfatter", "Jens Petrius")))
      ))

    val privatePath = repository.insert(
      DefaultLearningPath.copy(
        copyright = Copyright("by", List(Author("forfatter", "Test testesen")))
      ))

    val publicContributors = repository.allPublishedContributors
    publicContributors should contain(Author("forfatter", "James Bond"))
    publicContributors should contain(Author("forfatter", "Christian Bond"))
    publicContributors should contain(Author("forfatter", "Jens Petrius"))
    publicContributors should not contain (Author("forfatter", "Test testesen"))

    repository.deletePath(publicPath.id.get)
    repository.deletePath(privatePath.id.get)
  }

  test("That allPublishedContributors removes duplicates") {
    val publicPath1 = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        copyright = Copyright("by",
                              List(Author("forfatter", "James Bond"),
                                   Author("forfatter", "Christian Bond"),
                                   Author("forfatter", "Jens Petrius")))
      ))
    val publicPath2 = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        copyright = Copyright("by", List(Author("forfatter", "James Bond"), Author("forfatter", "Test testesen")))))

    val publicContributors = repository.allPublishedContributors
    publicContributors should contain(Author("forfatter", "James Bond"))
    publicContributors should contain(Author("forfatter", "Christian Bond"))
    publicContributors should contain(Author("forfatter", "Jens Petrius"))
    publicContributors should contain(Author("forfatter", "Test testesen"))

    publicContributors.count(_.name == "James Bond") should be(1)

    repository.deletePath(publicPath1.id.get)
    repository.deletePath(publicPath2.id.get)
  }

  test("That only learningsteps with status ACTIVE are returned together with a learningpath") {
    val learningPath = repository.insert(DefaultLearningPath)
    val activeStep1 = repository.insertLearningStep(DefaultLearningStep.copy(learningPathId = learningPath.id))
    val activeStep2 = repository.insertLearningStep(DefaultLearningStep.copy(learningPathId = learningPath.id))
    val deletedStep = repository.insertLearningStep(
      DefaultLearningStep.copy(learningPathId = learningPath.id, status = StepStatus.DELETED))

    learningPath.id.isDefined should be(true)
    val savedLearningPath = repository.withId(learningPath.id.get)
    savedLearningPath.isDefined should be(true)
    savedLearningPath.get.learningsteps.size should be(2)
    savedLearningPath.get.learningsteps
      .forall(_.status == StepStatus.ACTIVE) should be(true)

    repository.deletePath(learningPath.id.get)
  }

  test("That getLearningPathByPage returns correct result when pageSize is smaller than amount of steps") {
    emptyTestDatabase

    val steps = List(
      DefaultLearningStep,
      DefaultLearningStep,
      DefaultLearningStep
    )

    val learningPath =
      repository.insert(DefaultLearningPath.copy(learningsteps = steps, status = LearningPathStatus.PUBLISHED))

    val page1 = repository.getLearningPathByPage(2, 0)
    val page2 = repository.getLearningPathByPage(2, 2)

    page1 should be(List(learningPath))
    page2 should be(List.empty)

    repository.deletePath(learningPath.id.get)
  }

  test("That getLeraningPathByPage returns only published results") {
    emptyTestDatabase

    val steps = List(
      DefaultLearningStep,
      DefaultLearningStep,
      DefaultLearningStep
    )

    val learningPath1 =
      repository.insert(DefaultLearningPath.copy(learningsteps = steps, status = LearningPathStatus.PRIVATE))
    val learningPath2 =
      repository.insert(DefaultLearningPath.copy(learningsteps = steps, status = LearningPathStatus.PRIVATE))
    val learningPath3 =
      repository.insert(DefaultLearningPath.copy(learningsteps = steps, status = LearningPathStatus.PUBLISHED))

    val page1 = repository.getLearningPathByPage(2, 0)
    val page2 = repository.getLearningPathByPage(2, 2)

    page1 should be(List(learningPath3))
    page2 should be(List.empty)

    repository.deletePath(learningPath1.id.get)
    repository.deletePath(learningPath2.id.get)
    repository.deletePath(learningPath3.id.get)
  }

  def emptyTestDatabase = {
    DB autoCommit (implicit session => {
      sql"delete from learningpathapi_test.learningpaths;".execute.apply()(session)
      sql"delete from learningpathapi_test.learningsteps;".execute.apply()(session)
    })
  }

  def deleteAllWithOwner(owner: String): Unit = {
    inTransaction { implicit session =>
      repository
        .withOwner(owner)
        .foreach(lp => repository.deletePath(lp.id.get))
    }
  }
}
