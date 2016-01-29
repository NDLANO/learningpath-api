package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.model.LearningPath


trait LearningpathData {


  def insert(learningpath: LearningPath): LearningPath
  def exists(learningPathId: Long):Boolean
  def update(learningpath: LearningPath): LearningPath
  def delete(learningPathId: Long)

  def withId(id: Long): Option[LearningPath]
  def withIdAndOwner(id: Long, owner: String): Option[LearningPath]
  def withStatus(status:String): List[LearningPath]
  def withStatusAndOwner(status:String, owner:String): List[LearningPath]
  def withIdAndStatus(id:Long, status:String): Option[LearningPath]
  def withIdStatusAndOwner(id:Long, status:String, owner:String): Option[LearningPath]
}
