package no.ndla.learningpathapi.integration

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.business.{LearningPathSearch, SearchIndexer}
import no.ndla.learningpathapi.service.ModelConverters.asApiLearningPathSummary
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.indices.IndexMissingException
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.native.Serialization.read

import scala.collection.mutable.ListBuffer
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
    val titleSearch = new ListBuffer[QueryDefinition]
    titleSearch += matchQuery("title.title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => titleSearch += termQuery("title.language", lang))

    val descSearch = new ListBuffer[QueryDefinition]
    descSearch += matchQuery("description.description", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => descSearch += termQuery("description.language", lang))

    val stepTitleSearch = new ListBuffer[QueryDefinition]
    stepTitleSearch += matchQuery("learningsteps.title.title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => stepTitleSearch += termQuery("learningsteps.title.language", lang))

    val stepDescSearch = new ListBuffer[QueryDefinition]
    stepDescSearch += matchQuery("learningsteps.description.description", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => stepDescSearch += termQuery("learningsteps.description.language", lang))

    val tagSearch = new ListBuffer[QueryDefinition]
    tagSearch += matchQuery("tags.tag", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => tagSearch += termQuery("tags.language", lang))

    val authorSearch = new ListBuffer[QueryDefinition]
    authorSearch += matchQuery("author.name", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)

    val theSearch = search in LearningpathApiProperties.SearchIndex -> LearningpathApiProperties.SearchDocument query {
      bool {
        should (
          nestedQuery("title").query {bool {must (titleSearch.toList)}},
          nestedQuery("description").query {bool {must (descSearch.toList)}},
          nestedQuery("learningsteps.title").query {bool {must (stepTitleSearch.toList)}},
          nestedQuery("learningsteps.description").query {bool {must (stepDescSearch.toList)}},
          nestedQuery("tags").query {bool {must (tagSearch.toList)}},
          nestedQuery("author").query {bool {must (authorSearch.toList)}}
        )
      }
    }
    theSearch.sort(field sort "id")
    executeSearch(theSearch, page, pageSize)
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
