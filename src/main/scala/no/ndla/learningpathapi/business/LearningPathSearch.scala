package no.ndla.learningpathapi.business

import no.ndla.learningpathapi.{SearchResult, LearningPathSummary}
import no.ndla.learningpathapi.model.Sort


trait LearningPathSearch {
  def all(sort: Sort.Value, language: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult
  def matchingQuery(query: Iterable[String], language: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int]): SearchResult
}
