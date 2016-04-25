package no.ndla.learningpathapi.repository

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.domain._
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

class LearningPathRepositoryComponentIntegrationTest extends IntegrationSuite with TestEnvironment {
  var repository: LearningPathRepository = _

  val DefaultLearningPath = LearningPath(
    None, None, None,
    List(Title("UNIT-TEST-1", None)),
    List(Description("UNIT-TEST", None)), None, None,
    LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.EXTERNAL,
    new Date(), List(), "UNIT-TEST")

  val DefaultLearningStep = LearningStep(
    None, None, None, None, 0,
    List(Title("UNIT-TEST", None)),
    List(Description("UNIT-TEST", None)),
    List(EmbedUrl("http://www.vg.no", None)),
    StepType.TEXT, None)

  override def beforeEach() = {
    repository = new LearningPathRepository()
  }

  override def beforeAll() = {
    val datasource = getDataSource()
    DBMigrator.migrate(datasource)
    ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
  }

  test("That insert, fetch and delete works happy-day") {
    inTransaction { implicit session =>
      val inserted = repository.insert(DefaultLearningPath)
      inserted.id.isDefined should be (right = true)

      val fetched = repository.withId(inserted.id.get)
      fetched.isDefined should be (right = true)
      fetched.get.id.get should equal (inserted.id.get)

      repository.delete(inserted.id.get)
    }
  }

  test("That transaction is rolled back if exception is thrown") {
    val owner = s"unit-test-${ System.currentTimeMillis() }"
    deleteAllWithOwner(owner)

    try {
      inTransaction { implicit session =>
        val inserted = repository.insert(DefaultLearningPath.copy(owner = owner))
        throw new RuntimeException("Provoking exception inside transaction")
      }
      fail("Exception should prevent normal execution")
    } catch {
      case t: Throwable => {
        repository.withOwner(owner).length should be (0)
      }
    }
  }

  test("That updating several times is not throwing optimistic locking exception") {
    val inserted = repository.insert(DefaultLearningPath)
    val firstUpdate = repository.update(inserted.copy(title = List(Title("First change", None))))
    val secondUpdate = repository.update(firstUpdate.copy(title = List(Title("Second change", None))))
    val thirdUpdate = repository.update(secondUpdate.copy(title = List(Title("Third change", None))))

    inserted.version should equal (Some(1))
    firstUpdate.version should equal (Some(2))
    secondUpdate.version should equal (Some(3))
    thirdUpdate.version should equal (Some(4))

    repository.delete(thirdUpdate.id.get)
  }

  test("That trying to update a learningPath with old version throws optimistic locking exception") {
    val inserted = repository.insert(DefaultLearningPath)
    val firstUpdate = repository.update(inserted.copy(title = List(Title("First change", None))))

    assertResult(s"Conflicting version is detected for learningPath with id = ${inserted.id} and version = ${inserted.version}") {
      intercept[OptimisticLockException] { repository.update(inserted.copy(title = List(Title("Second change, but with old version", None)))) }.getMessage
    }

    repository.delete(inserted.id.get)
  }

  test("That trying to update a learningStep with old version throws optimistic locking exception") {
    val learningPath = repository.insert(DefaultLearningPath)
    val insertedStep = repository.insertLearningStep(DefaultLearningStep.copy(learningPathId = learningPath.id))
    val firstUpdate = repository.updateLearningStep(insertedStep.copy(title = List(Title("First change", None))))

    assertResult(s"Conflicting version is detected for learningStep with id = ${insertedStep.id} and version = ${insertedStep.version}") {
      intercept[OptimisticLockException] { repository.updateLearningStep(insertedStep.copy(title = List(Title("First change", None)))) }.getMessage
    }

    repository.delete(learningPath.id.get)
  }

  def deleteAllWithOwner(owner: String) = {
    inTransaction{ implicit session =>
      repository.withOwner(owner).foreach(lp => repository.delete(lp.id.get))
    }
  }
}
