/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.api.LearningPathSummaryV2

case class SearchResult(
    totalCount: Long,
    page: Option[Int],
    pageSize: Int,
    language: String,
    results: Seq[LearningPathSummaryV2],
    scrollId: Option[String]
)
