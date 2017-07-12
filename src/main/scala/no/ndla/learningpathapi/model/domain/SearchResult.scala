/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain
import io.searchbox.core

case class SearchResult(totalCount: Long, page: Int, pageSize: Int, language: String, response: core.SearchResult)

