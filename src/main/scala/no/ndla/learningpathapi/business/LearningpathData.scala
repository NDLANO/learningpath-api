package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.model.{LearningPathStatus, LearningStep, LearningPath}


trait LearningpathData {

  def exists(learningPathId: Long):Boolean
  def exists(learningPathId: Long, learningStepId: Long): Boolean

  def insert(learningpath: LearningPath): LearningPath
  def insertLearningStep(learningStep: LearningStep): LearningStep

  def update(learningpath: LearningPath): LearningPath
  def updateLearningStep(learningStep: LearningStep): LearningStep

  def delete(learningPathId: Long)
  def deleteLearningStep(learningStepId: Long)

  def withId(id: Long): Option[LearningPath]
  def withExternalId(id: Option[String]): Option[LearningPath]
  def withStatus(status:LearningPathStatus.Value): List[LearningPath]
  def withStatusAndOwner(status:LearningPathStatus.Value, owner:String): List[LearningPath]

  def learningStepsFor(learningPathId: Long): List[LearningStep]
  def learningStepWithId(learningPathId: Long, learningStepId: Long): Option[LearningStep]
  def learningStepWithExternalId(externalId: Option[String]): Option[LearningStep]

  def minMaxId: (Long,Long)
  def learningPathsWithIdBetween(min: Long, max:Long): List[LearningPath]

}
