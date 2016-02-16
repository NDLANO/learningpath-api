package no.ndla.learningpathapi.business

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.AmazonIntegration

object SearchIndexer extends LazyLogging{

  val learningPathData = AmazonIntegration.getLearningpathData()
  val learningPathIndex = AmazonIntegration.getLearningPathIndex()


  def indexDocuments() = {
    synchronized {
      val start = System.currentTimeMillis

      val newIndexName = learningPathIndex.createNewIndex()
      val oldIndexName = learningPathIndex.aliasTarget

      oldIndexName match {
        case None => learningPathIndex.updateAliasTarget(oldIndexName, newIndexName)
        case Some(_) =>
      }

      var numIndexed = 0
      getRanges().foreach(range => {
        numIndexed += learningPathIndex.indexLearningPaths(learningPathData.learningPathsWithIdBetween(range._1, range._2), newIndexName)
      })

      oldIndexName.foreach(indexName => {
        learningPathIndex.updateAliasTarget(oldIndexName, newIndexName)
        learningPathIndex.removeIndex(indexName)
      })

      val result = s"Completed indexing of $numIndexed documents in ${System.currentTimeMillis() - start} ms."
      logger.info(result)
      result
    }
  }

  def getRanges():Iterator[(Long,Long)] = {
    val (minId, maxId) = learningPathData.minMaxId
    Seq.range(minId, maxId+1).grouped(LearningpathApiProperties.IndexBulkSize).map(group => (group.head, group.last))
  }
}
