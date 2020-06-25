/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.repository

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.integration.DataSource
import no.ndla.learningpathapi.model.domain._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.Try

trait LearningPathRepositoryComponent extends LazyLogging {
  this: DataSource =>
  val learningPathRepository: LearningPathRepository

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null): A = {
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
      new EnumNameSerializer(StepType) +
      new EnumNameSerializer(StepStatus) +
      new EnumNameSerializer(EmbedType) +
      new EnumNameSerializer(LearningPathStatus) +
      new EnumNameSerializer(LearningPathVerificationStatus) +
      new EnumNameSerializer(StepType) ++
      org.json4s.ext.JodaTimeSerializers.all

    def withId(id: Long)(implicit session: DBSession = AutoSession): Option[LearningPath] = {
      learningPathWhere(sqls"lp.id = $id AND lp.document->>'status' <> ${LearningPathStatus.DELETED.toString}")
    }

    def withIdIncludingDeleted(id: Long)(implicit session: DBSession = AutoSession): Option[LearningPath] = {
      learningPathWhere(sqls"lp.id = $id")
    }

    def withExternalId(externalId: String): Option[LearningPath] = {
      learningPathWhere(sqls"lp.external_id = $externalId")
    }

    def withOwner(owner: String): List[LearningPath] = {
      learningPathsWhere(
        sqls"lp.document->>'owner' = $owner AND lp.document->>'status' <> ${LearningPathStatus.DELETED.toString}")
    }

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
      sql"select id from learningpaths where external_id = ${externalId}"
        .map(rs => rs.long("id"))
        .single()
        .apply()
    }

    def learningPathsWithIsBasedOn(isBasedOnId: Long): List[LearningPath] = {
      learningPathsWhere(sqls"lp.document->>'isBasedOn' = ${isBasedOnId.toString()}")
    }

    def learningStepsFor(learningPathId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[LearningStep] = {
      val ls = LearningStep.syntax("ls")
      sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId"
        .map(LearningStep(ls.resultName))
        .list()
        .apply()
    }

    def learningStepWithId(learningPathId: Long, learningStepId: Long)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[LearningStep] = {
      val ls = LearningStep.syntax("ls")
      sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId and ${ls.id} = $learningStepId"
        .map(LearningStep(ls.resultName))
        .single()
        .apply()
    }

    def learningStepWithExternalIdAndForLearningPath(externalId: Option[String], learningPathId: Option[Long])(
        implicit session: DBSession = ReadOnlyAutoSession): Option[LearningStep] = {
      externalId.isEmpty || learningPathId.isEmpty match {
        case true => None
        case false => {
          val ls = LearningStep.syntax("ls")
          sql"select ${ls.result.*} from ${LearningStep.as(ls)} where ${ls.externalId} = ${externalId.get} and ${ls.learningPathId} = ${learningPathId.get}"
            .map(LearningStep(ls.resultName))
            .single()
            .apply()
        }
      }
    }

    def insert(learningpath: LearningPath)(implicit session: DBSession = AutoSession): LearningPath = {
      val startRevision = 1
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningpath))

      val learningPathId: Long =
        sql"insert into learningpaths(external_id, document, revision) values(${learningpath.externalId}, $dataObject, $startRevision)"
          .updateAndReturnGeneratedKey()
          .apply

      val learningSteps = learningpath.learningsteps.map(lsteps =>
        lsteps.map(learningStep => {
          insertLearningStep(learningStep.copy(learningPathId = Some(learningPathId)))
        }))

      logger.info(s"Inserted learningpath with id $learningPathId")
      learningpath.copy(id = Some(learningPathId), revision = Some(startRevision), learningsteps = learningSteps)
    }

    def insertWithImportId(learningpath: LearningPath, importId: String)(
        implicit session: DBSession = AutoSession): LearningPath = {
      val startRevision = 1
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningpath))

      val importIdUUID = Try(UUID.fromString(importId)).toOption
      val learningPathId: Long =
        sql"insert into learningpaths(external_id, document, revision, import_id) values(${learningpath.externalId}, $dataObject, $startRevision, $importIdUUID)"
          .updateAndReturnGeneratedKey()
          .apply
      val learningSteps = learningpath.learningsteps.map(lsteps =>
        lsteps.map(learningStep => {
          insertLearningStep(learningStep.copy(learningPathId = Some(learningPathId)))
        }))

      logger.info(s"Inserted learningpath with id $learningPathId")
      learningpath.copy(id = Some(learningPathId), revision = Some(startRevision), learningsteps = learningSteps)
    }

    def idAndimportIdOfLearningpath(externalId: String)(
        implicit session: DBSession = AutoSession): Option[(Long, Option[String])] = {
      val lp = LearningPath.syntax("lp")
      sql"""select id, import_id
            from ${LearningPath.as(lp)}
            where lp.document is not NULL and lp.external_id = $externalId"""
        .map(rs => (rs.long("id"), rs.stringOpt("import_id")))
        .single
        .apply()
    }

    def insertLearningStep(learningStep: LearningStep)(implicit session: DBSession = AutoSession): LearningStep = {
      val startRevision = 1
      val stepObject = new PGobject()
      stepObject.setType("jsonb")
      stepObject.setValue(write(learningStep))

      val learningStepId: Long =
        sql"insert into learningsteps(learning_path_id, external_id, document, revision) values (${learningStep.learningPathId}, ${learningStep.externalId}, $stepObject, $startRevision)"
          .updateAndReturnGeneratedKey()
          .apply()
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
      val count =
        sql"update learningpaths set document = $dataObject, revision = ${newRevision} where id = ${learningpath.id} and revision = ${learningpath.revision}"
          .update()
          .apply

      if (count != 1) {
        val msg =
          s"Conflicting revision is detected for learningPath with id = ${learningpath.id} and revision = ${learningpath.revision}"
        logger.warn(msg)
        throw new OptimisticLockException(msg)
      }

      logger.info(s"Updated learningpath with id ${learningpath.id}")
      learningpath.copy(revision = Some(newRevision))
    }

    def updateWithImportId(learningpath: LearningPath, importId: String)(
        implicit session: DBSession = AutoSession): LearningPath = {
      if (learningpath.id.isEmpty) {
        throw new RuntimeException("A non-persisted learningpath cannot be updated without being saved first.")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(learningpath))

      val importIdUUID = Try(UUID.fromString(importId)).toOption
      val newRevision = learningpath.revision.getOrElse(0) + 1
      val count =
        sql"update learningpaths set document = $dataObject, revision = ${newRevision}, import_id = $importIdUUID where id = ${learningpath.id} and revision = ${learningpath.revision}"
          .update()
          .apply

      if (count != 1) {
        val msg =
          s"Conflicting revision is detected for learningPath with id = ${learningpath.id} and revision = ${learningpath.revision}"
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
      val count =
        sql"update learningsteps set document = $dataObject, revision = ${newRevision} where id = ${learningStep.id} and revision = ${learningStep.revision}"
          .update()
          .apply
      if (count != 1) {
        val msg =
          s"Conflicting revision is detected for learningStep with id = ${learningStep.id} and revision = ${learningStep.revision}"
        logger.warn(msg)
        throw new OptimisticLockException(msg)
      }

      logger.info(s"Updated learningstep with id ${learningStep.id}")
      learningStep.copy(revision = Some(newRevision))
    }

    def deletePath(learningPathId: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from learningpaths where id = $learningPathId".update().apply
    }

    def deleteStep(learningStepId: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from learningsteps where id = $learningStepId".update().apply
    }

    def learningPathsWithIdBetween(min: Long, max: Long)(
        implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      val status = LearningPathStatus.PUBLISHED.toString

      sql"""select ${lp.result.*}, ${ls.result.*}
               from ${LearningPath.as(lp)}
               left join ${LearningStep.as(ls)} on ${lp.id} = ${ls.learningPathId}
               where lp.document->>'status' = $status
               and lp.id between $min and $max"""
        .one(LearningPath(lp.resultName))
        .toMany(LearningStep.opt(ls.resultName))
        .map { (learningpath, learningsteps) =>
          learningpath.copy(learningsteps = Some(learningsteps.toSeq))
        }
        .toList()
        .apply()
    }

    def minMaxId(implicit session: DBSession = ReadOnlyAutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from learningpaths"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single()
        .apply() match {
        case Some(minmax) => minmax
        case None         => (0L, 0L)
      }
    }

    def allPublishedTags(implicit session: DBSession = ReadOnlyAutoSession): List[LearningPathTags] = {
      val allTags =
        sql"""select document->>'tags' from learningpaths where document->>'status' = ${LearningPathStatus.PUBLISHED.toString}"""
          .map(rs => {
            rs.string(1)
          })
          .list()
          .apply()

      allTags
        .flatMap(tag => {
          parse(tag).extract[List[LearningPathTags]]
        })
        .groupBy(_.language)
        .map(entry => LearningPathTags(entry._2.flatMap(_.tags).distinct.sorted, entry._1))
        .toList
    }

    def allPublishedContributors(implicit session: DBSession = ReadOnlyAutoSession): List[Author] = {
      val allCopyrights =
        sql"""select document->>'copyright' from learningpaths where document->>'status' = ${LearningPathStatus.PUBLISHED.toString}"""
          .map(rs => {
            rs.string(1)
          })
          .list()
          .apply()

      allCopyrights
        .map(copyright => {
          parse(copyright).extract[Copyright]
        })
        .flatMap(_.contributors)
        .distinct
        .sortBy(_.name)
    }

    private def learningPathsWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      sql"select ${lp.result.*}, ${ls.result.*} from ${LearningPath.as(lp)} left join ${LearningStep
        .as(ls)} on ${lp.id} = ${ls.learningPathId} where $whereClause"
        .one(LearningPath(lp.resultName))
        .toMany(LearningStep.opt(ls.resultName))
        .map { (learningpath, learningsteps) =>
          learningpath.copy(learningsteps = Some(learningsteps.filter(_.status == StepStatus.ACTIVE).toSeq))
        }
        .list
        .apply()
    }

    private def learningPathWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      sql"select ${lp.result.*}, ${ls.result.*} from ${LearningPath.as(lp)} left join ${LearningStep
        .as(ls)} on ${lp.id} = ${ls.learningPathId} where $whereClause"
        .one(LearningPath(lp.resultName))
        .toMany(LearningStep.opt(ls.resultName))
        .map { (learningpath, learningsteps) =>
          learningpath.copy(learningsteps = Some(learningsteps.filter(_.status == StepStatus.ACTIVE).toSeq))
        }
        .single
        .apply()
    }

    def getLearningPathByPage(pageSize: Int, offset: Int)(
        implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      val lps = SubQuery.syntax("lps").include(lp)
      sql"""
            select ${lps.resultAll}, ${ls.resultAll} from (select ${lp.resultAll}
                                                           from ${LearningPath.as(lp)}
                                                           where document#>>'{status}' = ${LearningPathStatus.PUBLISHED.toString}
                                                           limit $pageSize
                                                           offset $offset) lps
            left join ${LearningStep.as(ls)} on ${lps(lp).id} = ${ls.learningPathId}
      """
        .one(LearningPath(lps(lp).resultName))
        .toMany(LearningStep.opt(ls.resultName))
        .map { (learningpath, learningsteps) =>
          learningpath.copy(learningsteps = Some(learningsteps.filter(_.status == StepStatus.ACTIVE).toSeq))
        }
        .list
        .apply()
    }

    def learningPathsWithStatus(status: LearningPathStatus.Value)(
        implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
      learningPathsWhere(sqls"lp.document#>>'{status}' = ${status.toString}")
    }

    def publishedLearningPathCount(implicit session: DBSession = ReadOnlyAutoSession): Long = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      sql"select count(*) from ${LearningPath.as(lp)} where document#>>'{status}' = ${LearningPathStatus.PUBLISHED.toString}"
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0)
    }

    def learningPathCount(implicit session: DBSession = ReadOnlyAutoSession): Long = {
      val (lp, ls) = (LearningPath.syntax("lp"), LearningStep.syntax("ls"))
      sql"select count(*) from ${LearningPath.as(lp)}"
        .map(rs => rs.long("count"))
        .single
        .apply()
        .getOrElse(0)
    }
  }

}
