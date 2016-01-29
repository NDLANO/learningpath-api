package no.ndla.learningpathapi.model

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties


case class LearningPath(id:Option[Long], title: List[Title], description: List[Description], learningsteps: List[LearningStep],
                        coverPhotoUrl: Option[String], duration: Int, status: String, verificationStatus: String, lastUpdated: Date, tags:List[LearningPathTag], owner: String) {

  def isPrivate: Boolean = {
    status == LearningpathApiProperties.Private
  }
}

case class LearningStep(id:Long, seqNo:Int, title:List[Title], description:List[Description], embedUrl:List[EmbedUrl], `type`:String, license:Option[String])
case class Title(title:String, language:Option[String])
case class EmbedUrl(url:String, language:Option[String])
case class Description(description:String, language:Option[String])
case class LearningPathTag(tag:String, language:Option[String])



