package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.integration.DatasourceComponent
import no.ndla.learningpathapi.model._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._


trait LearningPathRepositoryComponent extends LazyLogging {
  this: DatasourceComponent =>
  val learningPathRepository: LearningPathRepository

  class LearningPathRepository {
    ConnectionPool.singleton(new DataSourceConnectionPool(datasource))

    implicit val formats = org.json4s.DefaultFormats +
      LearningPath.JSonSerializer +
      LearningStep.JSonSerializer +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType)

    def exists(learningPathId: Long): Boolean = {
      DB readOnly {implicit session =>
        sql"select exists(select 1 from learningpaths where id=$learningPathId)".map(rs => rs.boolean(1)).single().apply match {
          case Some(t) => t
          case None => false
        }
      }
    }

    def exists(learningPathId: Long, learningStepId: Long): Boolean = {
      DB readOnly {implicit session =>
        sql"select exists(select 1 from learningsteps where id=$learningStepId and learning_path_id = $learningPathId)".map(rs => rs.boolean(1)).single().apply match {
          case Some(t) => t
          case None => false
        }
      }
    }

    def withId(id: Long): Option[LearningPath] = {
      learningPathWhere(sqls"lp.id = $id")
    }

    def withExternalId(externalId: Option[String]): Option[LearningPath] = {
      externalId match {
        case None => None
        case Some(extId) => learningPathWhere(sqls"lp.external_id = $extId")
      }
    }

    def withStatus(status: LearningPathStatus.Value): List[LearningPath] = {
      learningPathsWhere(sqls"lp.document->>'status' = ${status.toString}")
    }

    def withStatusAndOwner(status: LearningPathStatus.Value, owner: String): List[LearningPath] = {
      learningPathsWhere(sqls"lp.document->>'status' = ${status.toString} and lp.document->>'owner' = $owner")
    }

    def learningStepsFor(learningPathId: Long): List[LearningStep] = {
      val ls = LearningStep.syntax("ls")
      DB readOnly { implicit session =>
        sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId".map(LearningStep(ls.resultName)).list().apply()
      }
    }

    def learningStepWithId(learningPathId: Long, learningStepId: Long): Option[LearningStep] = {
      val ls = LearningStep.syntax("ls")
      DB readOnly { implicit session =>
        sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId and ${ls.id} = $learningStepId".map(LearningStep(ls.resultName)).single().apply()
      }
    }

    def learningStepWithExternalId(externalId: Option[String]): Option[LearningStep] = {
      externalId match {
        case None => None
        case Some(extId) => {
          val ls = LearningStep.syntax("ls")
          DB readOnly { implicit session =>
            sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.externalId} = $extId".map(LearningStep(ls.resultName)).single().apply()
          }
        }
      }
    }

    def insert(learningpath: LearningPath): LearningPath = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningpath))

      DB localTx {implicit session =>
        val learningPathId:Long = sql"insert into learningpaths(external_id, document) values(${learningpath.externalId}, $dataObject)".updateAndReturnGeneratedKey().apply

        val learningSteps = learningpath.learningsteps.map(learningStep => {
          insertLearningStepNoTx(learningStep.copy(learningPathId = Some(learningPathId)))
        })

        logger.info(s"Inserted learningpath with id $learningPathId")
        learningpath.copy(id = Some(learningPathId), learningsteps = learningSteps)
      }
    }

    def insertLearningStep(learningStep: LearningStep): LearningStep = {
      DB localTx {implicit session =>
        insertLearningStepNoTx(learningStep)
      }
    }

    def update(learningpath: LearningPath): LearningPath = {
      if(learningpath.id.isEmpty) {
        throw new RuntimeException("A non-persisted learningpath cannot be updated without being saved first.")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningpath))

      DB localTx {implicit session =>
        sql"update learningpaths set document = $dataObject where id = ${learningpath.id}".update().apply
        logger.info(s"Updated learningpath with id ${learningpath.id}")
        learningpath
      }
    }

    def updateLearningStep(learningStep: LearningStep): LearningStep = {
      if(learningStep.id.isEmpty) {
        throw new RuntimeException("A non-persisted learningStep cannot be updated without being saved first.")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningStep))

      DB localTx { implicit session =>
        sql"update learningsteps set document = $dataObject where id = ${learningStep.id}".update().apply
        logger.info(s"Updated learningstep with id ${learningStep.id}")
        learningStep
      }
    }

    def delete(learningPathId: Long) = {
      DB localTx {implicit session =>
        sql"delete from learningpaths where id = $learningPathId".update().apply
      }
    }

    def deleteLearningStep(learningStepId: Long): Unit = {
      DB localTx {implicit session =>
        sql"delete from learningsteps where id = $learningStepId".update().apply
      }
    }

    def learningPathsWithIdBetween(min: Long, max: Long): List[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      val status = LearningPathStatus.PUBLISHED.toString

      DB readOnly {implicit session =>
        sql"""select ${lp.result.*}, ${ls.result.*}
               from ${LearningPath.as(lp)}
               left join ${LearningStep.as(ls)} on ${lp.id} = ${ls.learningPathId}
               where lp.document->>'status' = $status
               and lp.id between $min and $max"""
          .one(LearningPath(lp.resultName))
          .toMany(LearningStep.opt(ls.resultName))
          .map{(learningpath, learningsteps) => learningpath.copy(learningsteps = learningsteps)}
          .toList().apply()
      }
    }

    def minMaxId: (Long,Long) = {
      DB readOnly { implicit session =>
        sql"select min(id) as mi, max(id) as ma from learningpaths".map(rs => {
          (rs.long("mi"),rs.long("ma"))
        }).single().apply() match {
          case Some(minmax) => minmax
          case None => (0L,0L)
        }
      }
    }


    private def learningPathsWhere(whereClause: SQLSyntax): List[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      DB readOnly{implicit session =>
        sql"select ${lp.result.*}, ${ls.result.*} from ${LearningPath.as(lp)} left join ${LearningStep.as(ls)} on ${lp.id} = ${ls.learningPathId} where $whereClause"
          .one(LearningPath(lp.resultName))
          .toMany(LearningStep.opt(ls.resultName))
          .map{(learningpath, learningsteps) => learningpath.copy(learningsteps = learningsteps)}
          .list.apply()
      }
    }

    private def learningPathWhere(whereClause: SQLSyntax): Option[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      DB readOnly{implicit session =>
        sql"select ${lp.result.*}, ${ls.result.*} from ${LearningPath.as(lp)} left join ${LearningStep.as(ls)} on ${lp.id} = ${ls.learningPathId} where $whereClause"
          .one(LearningPath(lp.resultName))
          .toMany(LearningStep.opt(ls.resultName))
          .map{(learningpath, learningsteps) => learningpath.copy(learningsteps = learningsteps)}
          .single.apply()
      }
    }

    private def insertLearningStepNoTx(learningStep: LearningStep)(implicit session: DBSession): LearningStep = {
      val stepObject = new PGobject()
      stepObject.setType("jsonb")
      stepObject.setValue(write(learningStep))

      val learningStepId:Long = sql"insert into learningsteps(learning_path_id, external_id, document) values (${learningStep.learningPathId}, ${learningStep.externalId}, $stepObject)".updateAndReturnGeneratedKey().apply
      logger.info(s"Inserted learningstep with id $learningStepId")
      learningStep.copy(id = Some(learningStepId))
    }
  }
}