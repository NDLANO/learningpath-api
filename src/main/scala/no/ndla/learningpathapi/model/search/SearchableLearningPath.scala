package no.ndla.learningpathapi.model.search

import java.util.Date

import no.ndla.learningpathapi.model.domain.{Description, LearningPathTag, Title}

case class SearchableLearningPath(id: Long, title: List[Title], description: List[Description], coverPhotoUrl: Option[String],
                                  duration: Option[Int], status: String, verificationStatus: String, lastUpdated: Date, tags: List[LearningPathTag],
                                  author: String, learningsteps: List[SearchableLearningStep])
