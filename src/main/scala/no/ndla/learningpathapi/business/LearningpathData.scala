package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.model.LearningPath


trait LearningpathData {

  def exists(learningpath: LearningPath):Boolean
  def insert(learningpath: LearningPath, userId:String): LearningPath
  def update(learningpath: LearningPath, userId:String): LearningPath

  def withStatus(status:String): List[LearningPath]
  def withStatusAndOwner(status:String, owner:String): List[LearningPath]
  def withIdAndStatus(id:Long, status:String): Option[LearningPath]
  def withIdStatusAndOwner(id:Long, status:String, owner:String): Option[LearningPath]
}
