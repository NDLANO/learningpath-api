package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.model.{LearningStep, LearningPath}


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
  def withIdAndOwner(id: Long, owner: String): Option[LearningPath]
  def withStatus(status:String): List[LearningPath]
  def withStatusAndOwner(status:String, owner:String): List[LearningPath]
  def withIdAndStatus(id:Long, status:String): Option[LearningPath]
  def withIdStatusAndOwner(id:Long, status:String, owner:String): Option[LearningPath]

  def learningStepsFor(learningPathId: Long): List[LearningStep]
  def learningStepWithId(learningPathId: Long, learningStepId: Long): Option[LearningStep]
}
