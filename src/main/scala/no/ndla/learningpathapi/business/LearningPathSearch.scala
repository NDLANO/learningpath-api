package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.LearningPathSummary
import no.ndla.learningpathapi.model.Sort


trait LearningPathSearch {
  def all(sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[LearningPathSummary]
  def matchingQuery(query: Iterable[String], language: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int]): Iterable[LearningPathSummary]
}
