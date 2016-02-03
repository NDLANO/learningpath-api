package no.ndla.learningpathapi.business

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.integration.AmazonIntegration

object SearchIndexer extends LazyLogging{

  val learningPathData = AmazonIntegration.getLearningpathData()
  val learningPathIndex = AmazonIntegration.getLearningPathIndex()


  def indexDocuments() = {
    synchronized {
      val start = System.currentTimeMillis

      val newIndexName = learningPathIndex.create()
      val oldIndexName = learningPathIndex.aliasTarget

      oldIndexName match {
        case None => learningPathIndex.updateAliasTarget(oldIndexName, newIndexName)
        case Some(_) =>
      }

      var numIndexed = 0
      learningPathData.applyToAllPublic(docs => {
        numIndexed += learningPathIndex.indexDocuments(docs, newIndexName)
      })

      oldIndexName.foreach(indexName => {
        learningPathIndex.updateAliasTarget(oldIndexName, newIndexName)
        learningPathIndex.delete(indexName)
      })

      val result = s"Completed indexing of $numIndexed documents in ${System.currentTimeMillis() - start} ms."
      logger.info(result)
      result
    }
  }
}
