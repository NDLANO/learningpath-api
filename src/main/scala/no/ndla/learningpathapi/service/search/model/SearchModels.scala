package no.ndla.learningpathapi.service.search.model

import no.ndla.learningpathapi.{LearningPathSummary, model}


case class SearchableLearningPath(id: String)
case class SearchableLearningStep(id: String)


object SearchConverter {
  def asApiLearningPathSummary(learningPath: SearchableLearningPath): LearningPathSummary = {
    null
  }

  def asSearchableLearningPath(learningPath: model.LearningPath): SearchableLearningPath = {
    null
  }
}
