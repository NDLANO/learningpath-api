package no.ndla.learningpathapi.business

import com.typesafe.scalalogging.LazyLogging
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
      learningPathData.applyToAllPublic(learningPaths => {
        numIndexed += learningPathIndex.indexLearningPaths(learningPaths, newIndexName)
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
}
