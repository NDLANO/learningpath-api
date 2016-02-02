package no.ndla.learningpathapi.business

import com.typesafe.scalalogging.LazyLogging

object SearchIndexer extends LazyLogging{

  def indexDocuments() = {
    val result = s"Completed indexing of X documents in X ms."
    logger.info(result)
    result
  }
}
