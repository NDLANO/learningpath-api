/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.search

case class SearchableLearningStep(stepType: String,
                                  embedUrl: List[String],
                                  titles: SearchableTitles,
                                  descriptions: SearchableDescriptions)
