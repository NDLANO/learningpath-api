package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.model.LearningPath

trait LearningPathIndex {
  def indexLearningPaths(docs: List[LearningPath], indexName: String): Int
  def indexLearningPath(learningPath: LearningPath)
  def deleteLearningPath(learningPath: LearningPath)

  def createNewIndex():String
  def removeIndex(indexName: String): Unit
  def aliasTarget:Option[String]
  def updateAliasTarget(oldIndexName:Option[String], indexName: String)
}
