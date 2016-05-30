package no.ndla.learningpathapi.repository

import java.util.Date

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.domain.{LearningPathTag, _}
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

    inserted.revision should equal (Some(1))
    firstUpdate.revision should equal (Some(2))
    secondUpdate.revision should equal (Some(3))
    thirdUpdate.revision should equal (Some(4))

    repository.delete(thirdUpdate.id.get)
  }

  test("That trying to update a learningPath with old revision number throws optimistic locking exception") {
    val inserted = repository.insert(DefaultLearningPath)
    val firstUpdate = repository.update(inserted.copy(title = List(Title("First change", None))))

    assertResult(s"Conflicting revision is detected for learningPath with id = ${inserted.id} and revision = ${inserted.revision}") {
      intercept[OptimisticLockException] { repository.update(inserted.copy(title = List(Title("Second change, but with old revision", None)))) }.getMessage
    }

    repository.delete(inserted.id.get)
  }

  test("That trying to update a learningStep with old revision throws optimistic locking exception") {
    val learningPath = repository.insert(DefaultLearningPath)
    val insertedStep = repository.insertLearningStep(DefaultLearningStep.copy(learningPathId = learningPath.id))
    val firstUpdate = repository.updateLearningStep(insertedStep.copy(title = List(Title("First change", None))))

    assertResult(s"Conflicting revision is detected for learningStep with id = ${insertedStep.id} and revision = ${insertedStep.revision}") {
      intercept[OptimisticLockException] { repository.updateLearningStep(insertedStep.copy(title = List(Title("First change", None)))) }.getMessage
    }

    repository.delete(learningPath.id.get)
  }

  test("That allPublishedTags returns only published tags") {
    val publicPath = repository.insert(DefaultLearningPath.copy(
      status = LearningPathStatus.PUBLISHED,
      tags = List(
        LearningPathTag("aaa", Some("nb")),
        LearningPathTag("bbb", Some("nn")),
        LearningPathTag("ccc", Some("en")))))

    val privatePath = repository.insert(DefaultLearningPath.copy(
      tags = List(LearningPathTag("ddd", Some("nb")))))

    val publicTags = repository.allPublishedTags
    publicTags should contain (LearningPathTag("aaa", Some("nb")))
    publicTags should contain (LearningPathTag("bbb", Some("nn")))
    publicTags should contain (LearningPathTag("ccc", Some("en")))
    publicTags should not contain (LearningPathTag("ddd", Some("nb")))

    repository.delete(publicPath.id.get)
    repository.delete(privatePath.id.get)
  }

  test("That allPublishedTags removes duplicates") {
    val publicPath1 = repository.insert(DefaultLearningPath.copy(status = LearningPathStatus.PUBLISHED, tags = List(LearningPathTag("aaa", Some("nb")), LearningPathTag("aaa", Some("nn")))))
    val publicPath2 = repository.insert(DefaultLearningPath.copy(status = LearningPathStatus.PUBLISHED, tags = List(LearningPathTag("aaa", Some("nb")), LearningPathTag("bbb", Some("nb")))))

    val publicTags = repository.allPublishedTags
    publicTags should contain (LearningPathTag("aaa", Some("nb")))
    publicTags should contain (LearningPathTag("aaa", Some("nn")))
    publicTags should contain (LearningPathTag("bbb", Some("nb")))
    publicTags.count(a => a.tag == "aaa" && a.language.contains("nb")) should be (1)

    repository.delete(publicPath1.id.get)
    repository.delete(publicPath2.id.get)
  }

  def deleteAllWithOwner(owner: String) = {
    inTransaction{ implicit session =>
      repository.withOwner(owner).foreach(lp => repository.delete(lp.id.get))
    }
  }
}
