package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.model.LearningPath

trait LearningPathIndex {
  def indexDocuments(docs: List[LearningPath], indexName: String): Int
  def create():String
  def delete(indexName: String): Unit
  def aliasTarget:Option[String]
  def updateAliasTarget(oldIndexName:Option[String], indexName: String)
}
