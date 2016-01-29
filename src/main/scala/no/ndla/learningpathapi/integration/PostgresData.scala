package no.ndla.learningpathapi.integration

import java.util.Date
import javax.sql.DataSource

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.business.LearningpathData
import no.ndla.learningpathapi.model.LearningPath
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}

class PostgresData(dataSource: DataSource) extends LearningpathData with LazyLogging {
  implicit val formats = org.json4s.DefaultFormats

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  override def exists(learningpath: LearningPath): Boolean = {
    learningpath.id match {
      case None => false
      case Some(x) => {
        DB readOnly {implicit session =>
          sql"select exists(select 1 from learningpaths where id=${x})".map(rs => (rs.boolean(1))).single.apply match {
            case Some(t) => t
            case None => false
          }
        }
      }
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
      sql"update learningpaths set document = ${dataObject} where id = ${learningpath.id}".update().apply
      logger.info(s"Updated learningpath with id ${learningpath.id}")
      learningpath
    }
  }

  override def withId(id: Long): Option[LearningPath] = {
    DB readOnly{implicit session =>
      sql"select id, document from learningpaths where id = ${id}".map(rs => asLearningPath(rs.long("id"), rs.string("document"))).single().apply()
    }
  }

  override def withIdAndOwner(id: Long, owner: String): Option[LearningPath] = {
    DB readOnly{implicit session =>
      sql"select id, document from learningpaths where id = ${id} and document->>'owner' = ${owner}".map(rs => asLearningPath(rs.long("id"), rs.string("document"))).single().apply()
    }
  }


  override def withIdAndStatus(id: Long, status: String): Option[LearningPath] = {
    DB readOnly{implicit session =>
      sql"select id, document from learningpaths where id = ${id} and document->>'status' = ${status}".map(rs => asLearningPath(rs.long("id"), rs.string("document"))).single().apply()
    }
  }

  override def withIdStatusAndOwner(id: Long, status: String, owner: String): Option[LearningPath] = {
    DB readOnly{implicit session =>
      sql"select id, document from learningpaths where id = ${id} and document->>'status' = ${status} and document->>'owner' = ${owner}".map(rs => asLearningPath(rs.long("id"), rs.string("document"))).single().apply()
    }
  }

  override def withStatus(status: String): List[LearningPath] = {
    DB readOnly{implicit session =>
      sql"""select id, document from learningpaths where document->>'status' = ${status} limit 100"""
        .map(rs => asLearningPath(rs.long("id"), rs.string("document")))
        .list().apply()
    }
  }

  override def withStatusAndOwner(status: String, owner: String): List[LearningPath] = {
    DB readOnly{implicit session =>
      sql"""select id, document from learningpaths where document->>'status' = ${status} and document->>'owner' = ${owner} limit 100"""
        .map(rs => asLearningPath(rs.long("id"), rs.string("document")))
        .list().apply()
    }
  }

  override def insert(learningpath: LearningPath): LearningPath = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(learningpath))

    DB localTx {implicit session =>
      val id:Long = sql"insert into learningpaths(document) values(${dataObject})".updateAndReturnGeneratedKey().apply
      logger.info(s"Inserted learningpath with id ${id}")
      LearningPath(Some(id),
        learningpath.title,
        learningpath.description,
        learningpath.learningsteps,
        learningpath.coverPhotoUrl,
        learningpath.duration,
        learningpath.status,
        learningpath.verificationStatus,
        learningpath.lastUpdated,
        learningpath.tags,
        learningpath.owner)
    }
  }

  def asLearningPath(id:Long, json:String): LearningPath = {
    implicit val formats = org.json4s.DefaultFormats
    val meta = read[LearningPath](json)
    LearningPath(
      Some(id),
      meta.title,
      meta.description,
      meta.learningsteps,
      meta.coverPhotoUrl,
      meta.duration,
      meta.status,
      meta.verificationStatus,
      meta.lastUpdated,
      meta.tags,
      meta.owner)
  }
}
