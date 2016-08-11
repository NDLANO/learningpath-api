/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent


trait SearchIndexBuilderServiceComponent extends LazyLogging {
  this: LearningPathRepositoryComponent with SearchIndexServiceComponent =>
  val searchIndexBuilderService: SearchIndexBuilderService

  class SearchIndexBuilderService {
    def indexDocuments() = {
      synchronized {
        val start = System.currentTimeMillis

        val newIndexName = searchIndexService.createNewIndex()
        val oldIndexName = searchIndexService.aliasTarget

        oldIndexName match {
          case None => searchIndexService.updateAliasTarget(oldIndexName, newIndexName)
          case Some(_) =>
        }

        var numIndexed = 0
        getRanges().foreach(range => {
          numIndexed += searchIndexService.indexLearningPaths(learningPathRepository.learningPathsWithIdBetween(range._1, range._2), newIndexName)
        })

        oldIndexName.foreach(indexName => {
          searchIndexService.updateAliasTarget(oldIndexName, newIndexName)
          searchIndexService.removeIndex(indexName)
        })

        val result = s"Completed indexing of $numIndexed documents in ${System.currentTimeMillis() - start} ms."
        logger.info(result)
        result
      }
    }

    def getRanges():Iterator[(Long,Long)] = {
      val (minId, maxId) = learningPathRepository.minMaxId
      Seq.range(minId, maxId+1).grouped(LearningpathApiProperties.IndexBulkSize).map(group => (group.head, group.last))
    }
  }
}
