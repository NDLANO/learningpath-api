package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.LearningPathSummary

trait LearningPathSearch {
  def all(page: Option[Int], pageSize: Option[Int]): Iterable[LearningPathSummary]
  def matchingQuery(query: Iterable[String], language: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[LearningPathSummary]
}
