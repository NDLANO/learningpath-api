package no.ndla.learningpathapi.integration

import javax.sql.DataSource

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.business.LearningpathData
import no.ndla.learningpathapi.model.{LearningPath, LearningStep}
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}

class PostgresData(dataSource: DataSource) extends LearningpathData with LazyLogging {
  implicit val formats = org.json4s.DefaultFormats

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  override def exists(learningPathId: Long): Boolean = {
    DB readOnly {implicit session =>
      sql"select exists(select 1 from learningpaths where id=$learningPathId)".map(rs => rs.boolean(1)).single().apply match {
        case Some(t) => t
        case None => false
      }
    }
  }

  override def exists(learningPathId: Long, learningStepId: Long): Boolean = {
    DB readOnly {implicit session =>
      sql"select exists(select 1 from learningsteps where id=$learningStepId and learning_path_id = $learningPathId)".map(rs => rs.boolean(1)).single().apply match {
        case Some(t) => t
        case None => false
      }
    }
  }

  override def withId(id: Long): Option[LearningPath] = {
    learningPathWhere(sqls"lp.id = $id")
  }

  override def withIdAndOwner(id: Long, owner: String): Option[LearningPath] = {
    learningPathWhere(sqls"lp.id = $id and lp.document->>'owner' = $owner")
  }

  override def withIdAndStatus(id: Long, status: String): Option[LearningPath] = {
    learningPathWhere(sqls"lp.id = $id and lp.document->>'status' = $status")
  }

  override def withIdStatusAndOwner(id: Long, status: String, owner: String): Option[LearningPath] = {
    learningPathWhere(sqls"lp.id = $id and lp.document->>'status' = $status and lp.document->>'owner' = $owner")
  }

  override def withStatus(status: String): List[LearningPath] = {
    learningPathsWhere(sqls"lp.document->>'status' = $status")
  }

  override def withStatusAndOwner(status: String, owner: String): List[LearningPath] = {
    learningPathsWhere(sqls"lp.document->>'status' = $status and lp.document->>'owner' = $owner")
  }

  override def learningStepsFor(learningPathId: Long): List[LearningStep] = {
    val ls = LearningStep.syntax("ls")
    DB readOnly { implicit session =>
      sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId".map(LearningStep(ls.resultName)).list().apply()
    }
  }

  override def learningStepWithId(learningPathId: Long, learningStepId: Long): Option[LearningStep] = {
    val ls = LearningStep.syntax("ls")
    DB readOnly { implicit session =>
      sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId and ${ls.id} = $learningStepId".map(LearningStep(ls.resultName)).single().apply()
    }
  }

  override def insert(learningpath: LearningPath): LearningPath = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(learningpath))

    DB localTx {implicit session =>
      val learningPathId:Long = sql"insert into learningpaths(document) values($dataObject)".updateAndReturnGeneratedKey().apply

      val learningSteps = learningpath.learningsteps.map(learningStep => {
        insertLearningStepNoTx(learningStep.copy(learningPathId = Some(learningPathId)))
      })

      logger.info(s"Inserted learningpath with id $learningPathId")
      LearningPath(Some(learningPathId),
        learningpath.title,
        learningpath.description,
        learningpath.coverPhotoUrl,
        learningpath.duration,
        learningpath.status,
        learningpath.verificationStatus,
        learningpath.lastUpdated,
        learningpath.tags,
        learningpath.owner,
        learningSteps)
    }
  }

  override def insertLearningStep(learningStep: LearningStep): LearningStep = {
    DB localTx {implicit session =>
      insertLearningStepNoTx(learningStep)
    }
  }

  override def update(learningpath: LearningPath): LearningPath = {
    // TODO: Get converting to JSON to not save id (this is our primary key)
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

  override def updateLearningStep(learningStep: LearningStep): LearningStep = {
    if(learningStep.id.isEmpty) {
      throw new RuntimeException("A non-persisted learningStep cannot be updated without being saved first.")
    }

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(learningStep))

    DB localTx { implicit session =>
      sql"update learningsteps set document = $dataObject where id = ${learningStep.id}".update().apply
      logger.info(s"Updated learningpath with id ${learningStep.id}")
      learningStep
    }
  }

  override def delete(learningPathId: Long) = {
    DB localTx {implicit session =>
      sql"delete from learningpaths where id = $learningPathId".update().apply
    }
  }

  override def deleteLearningStep(learningStepId: Long): Unit = {
    DB localTx {implicit session =>
      sql"delete from learningsteps where id = $learningStepId".update().apply
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

    val learningStepId:Long = sql"insert into learningsteps(learning_path_id, document) values (${learningStep.learningPathId}, $stepObject)".updateAndReturnGeneratedKey().apply
    logger.info(s"Inserted learningstep with id $learningStepId")
    LearningStep(Some(learningStepId),
      learningStep.learningPathId,
      learningStep.seqNo,
      learningStep.title,
      learningStep.description,
      learningStep.embedUrl,
      learningStep.`type`,
      learningStep.license)
  }
}
