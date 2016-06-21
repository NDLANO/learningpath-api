package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.integration.DatasourceComponent
import no.ndla.learningpathapi.model.domain._
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

trait LearningPathRepositoryComponent extends LazyLogging {
  this: DatasourceComponent =>
  val learningPathRepository: LearningPathRepository

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null):A = {
    Option(session) match {
      case Some(x) => work(x)
      case None => {
        DB localTx { implicit newSession =>
          work(newSession)
        }
      }
    }
  }

  class LearningPathRepository {
    implicit val formats = org.json4s.DefaultFormats +
      LearningPath.JSonSerializer +
      LearningStep.JSonSerializer +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType)

    def withId(id: Long)(implicit session: DBSession = AutoSession): Option[LearningPath] = {
      learningPathWhere(sqls"lp.id = $id")
    }

    def withExternalId(externalId: Option[String]): Option[LearningPath] = {
      externalId match {
        case None => None
        case Some(extId) => learningPathWhere(sqls"lp.external_id = $extId")
      }
    }

    def withOwner(owner: String): List[LearningPath] = {
      learningPathsWhere(sqls"lp.document->>'owner' = $owner")
    }

    def learningStepsFor(learningPathId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[LearningStep] = {
      val ls = LearningStep.syntax("ls")
      sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId".map(LearningStep(ls.resultName)).list().apply()
    }

    def learningStepWithId(learningPathId: Long, learningStepId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[LearningStep] = {
      val ls = LearningStep.syntax("ls")
      sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId and ${ls.id} = $learningStepId".map(LearningStep(ls.resultName)).single().apply()
    }

    def learningStepWithExternalIdAndForLearningPath(externalId: Option[String], learningPathId: Option[Long])(implicit session: DBSession = ReadOnlyAutoSession): Option[LearningStep] = {
      externalId.isEmpty || learningPathId.isEmpty match {
        case true => None
        case false => {
          val ls = LearningStep.syntax("ls")
          sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.externalId} = ${externalId.get} and ${ls.learningPathId} = ${learningPathId.get}".map(LearningStep(ls.resultName)).single().apply()
        }
      }
    }

    def insert(learningpath: LearningPath)(implicit session: DBSession = AutoSession): LearningPath = {
      val startRevision = 1
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningpath))

      val learningPathId: Long = sql"insert into learningpaths(external_id, document, revision) values(${learningpath.externalId}, $dataObject, $startRevision)".updateAndReturnGeneratedKey().apply
      val learningSteps = learningpath.learningsteps.map(learningStep => {
        insertLearningStep(learningStep.copy(learningPathId = Some(learningPathId)))
      })

      logger.info(s"Inserted learningpath with id $learningPathId")
      learningpath.copy(id = Some(learningPathId), revision = Some(startRevision), learningsteps = learningSteps)
    }

    def insertLearningStep(learningStep: LearningStep)(implicit session: DBSession = AutoSession): LearningStep = {
      val startRevision = 1
      val stepObject = new PGobject()
      stepObject.setType("jsonb")
      stepObject.setValue(write(learningStep))

      val learningStepId: Long = sql"insert into learningsteps(learning_path_id, external_id, document, revision) values (${learningStep.learningPathId}, ${learningStep.externalId}, $stepObject, $startRevision)".updateAndReturnGeneratedKey().apply()
      logger.info(s"Inserted learningstep with id $learningStepId")
      learningStep.copy(id = Some(learningStepId), revision = Some(startRevision))
    }

    def update(learningpath: LearningPath)(implicit session: DBSession = AutoSession): LearningPath = {
      if (learningpath.id.isEmpty) {
        throw new RuntimeException("A non-persisted learningpath cannot be updated without being saved first.")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningpath))

      val newRevision = learningpath.revision.getOrElse(0) + 1
      val count = sql"update learningpaths set document = $dataObject, revision = ${newRevision} where id = ${learningpath.id} and revision = ${learningpath.revision}".update().apply

      if(count != 1) {
        val msg = s"Conflicting revision is detected for learningPath with id = ${learningpath.id} and revision = ${learningpath.revision}"
        logger.warn(msg)
        throw new OptimisticLockException(msg)
      }

      logger.info(s"Updated learningpath with id ${learningpath.id}")
      learningpath.copy(revision = Some(newRevision))
    }

    def updateLearningStep(learningStep: LearningStep)(implicit session: DBSession = AutoSession): LearningStep = {
      if (learningStep.id.isEmpty) {
        throw new RuntimeException("A non-persisted learningStep cannot be updated without being saved first.")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningStep))

      val newRevision = learningStep.revision.getOrElse(0) + 1
      val count = sql"update learningsteps set document = $dataObject, revision = ${newRevision} where id = ${learningStep.id} and revision = ${learningStep.revision}".update().apply
      if(count != 1) {
        val msg = s"Conflicting revision is detected for learningStep with id = ${learningStep.id} and revision = ${learningStep.revision}"
        logger.warn(msg)
        throw new OptimisticLockException(msg)
      }

      logger.info(s"Updated learningstep with id ${learningStep.id}")
      learningStep.copy(revision = Some(newRevision))
    }

    def delete(learningPathId: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from learningpaths where id = $learningPathId".update().apply
    }

    def learningPathsWithIdBetween(min: Long, max: Long)(implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      val status = LearningPathStatus.PUBLISHED.toString

        sql"""select ${lp.result.*}, ${ls.result.*}
               from ${LearningPath.as(lp)}
               left join ${LearningStep.as(ls)} on ${lp.id} = ${ls.learningPathId}
               where lp.document->>'status' = $status
               and lp.id between $min and $max"""
          .one(LearningPath(lp.resultName))
          .toMany(LearningStep.opt(ls.resultName))
          .map { (learningpath, learningsteps) => learningpath.copy(learningsteps = learningsteps) }
          .toList().apply()
    }

    def minMaxId(implicit session: DBSession = ReadOnlyAutoSession): (Long, Long) = {
        sql"select min(id) as mi, max(id) as ma from learningpaths".map(rs => {
          (rs.long("mi"), rs.long("ma"))
        }).single().apply() match {
          case Some(minmax) => minmax
          case None => (0L, 0L)
        }
    }

    def allPublishedTags(implicit session: DBSession = ReadOnlyAutoSession): List[LearningPathTag] = {
      val allTags = sql"""select document->>'tags' from learningpaths where document->>'status' = ${LearningPathStatus.PUBLISHED.toString}""".map(rs => {
        rs.string(1)
      }).list().apply()

      val liste:List[LearningPathTag] = allTags.map(s => {
        val json = org.json4s.native.JsonMethods.parse(s)
        for {
          JObject(child) <- json
          JField("tag", JString(tag)) <- child
          JField("language", JString(language)) <- child
        } yield LearningPathTag(tag, Some(language))
      }).flatten.distinct

      liste.sortBy(_.tag)
    }

    private def learningPathsWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
        sql"select ${lp.result.*}, ${ls.result.*} from ${LearningPath.as(lp)} left join ${LearningStep.as(ls)} on ${lp.id} = ${ls.learningPathId} where $whereClause"
          .one(LearningPath(lp.resultName))
          .toMany(LearningStep.opt(ls.resultName))
          .map { (learningpath, learningsteps) => learningpath.copy(learningsteps = learningsteps.filter(_.status == StepStatus.ACTIVE)) }
          .list.apply()
    }

    private def learningPathWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      sql"select ${lp.result.*}, ${ls.result.*} from ${LearningPath.as(lp)} left join ${LearningStep.as(ls)} on ${lp.id} = ${ls.learningPathId} where $whereClause"
        .one(LearningPath(lp.resultName))
        .toMany(LearningStep.opt(ls.resultName))
        .map { (learningpath, learningsteps) => learningpath.copy(learningsteps = learningsteps.filter(_.status == StepStatus.ACTIVE)) }
        .single.apply()
    }
  }

}
