package no.ndla.learningpathapi.integration

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.business.{LearningPathSearch, SearchIndexer}
import no.ndla.learningpathapi.service.ModelConverters.asApiLearningPathSummary
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.indices.IndexMissingException
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.native.Serialization.read

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ElasticLearningPathSearch(clusterName: String, clusterHost: String, clusterPort: String) extends LearningPathSearch with LazyLogging {

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build()
  val client = ElasticClient.remote(settings, ElasticsearchClientUri(s"elasticsearch://$clusterHost:$clusterPort"))

  implicit object ContentHitAs extends HitAs[LearningPathSummary] {
    override def as(hit: RichSearchHit): LearningPathSummary = {
      implicit val formats = org.json4s.DefaultFormats
      asApiLearningPathSummary(read[LearningPath](hit.sourceAsString))
    }
  }

  override def all(page: Option[Int], pageSize: Option[Int]): Iterable[LearningPathSummary] = {
    val theSearch = search in LearningpathApiProperties.SearchIndex -> LearningpathApiProperties.SearchDocument
    theSearch.sort(field sort "id")
    executeSearch(theSearch, page, pageSize)
  }

  override def matchingQuery(query: Iterable[String], language: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[LearningPathSummary] = {
    all(page, pageSize)
  }

  def executeSearch(search: SearchDefinition, page: Option[Int], pageSize: Option[Int]): Iterable[LearningPathSummary] = {
    val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
    try {
      client.execute {
        search start startAt limit numResults
      }.await.as[LearningPathSummary]
    } catch {
      case e: RemoteTransportException => errorHandler(e.getCause)
    }
  }

  def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
    val numResults = pageSize match {
      case Some(num) =>
        if (num > 0) num.min(LearningpathApiProperties.MaxPageSize) else LearningpathApiProperties.DefaultPageSize
      case None => LearningpathApiProperties.DefaultPageSize
    }

    val startAt = page match {
      case Some(sa) => (sa - 1).max(0) * numResults
      case None => 0
    }

    (startAt, numResults)
  }

  def errorHandler(exception: Throwable) = {
    exception match {
      case ex: IndexMissingException =>
        logger.error(ex.getDetailedMessage)
        scheduleIndexDocuments()
        throw ex
      case _ => throw exception
    }
  }

  def scheduleIndexDocuments() = {
    val f = Future {
      SearchIndexer.indexDocuments()
    }
    f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
  }
}
