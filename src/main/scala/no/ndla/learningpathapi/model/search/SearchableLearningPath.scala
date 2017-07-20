/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.search

import java.util.Date
import no.ndla.learningpathapi.model.domain.{Title}
import no.ndla.learningpathapi.model.api.{Copyright}

case class SearchableLearningPath(id: Long,
                                  titles: SearchableTitles,
                                  descriptions: SearchableDescriptions,
                                  coverPhotoUrl: Option[String],
                                  duration: Option[Int],
                                  status: String,
                                  verificationStatus: String,
                                  lastUpdated: Date,
                                  tags: SearchableTags,
                                  learningsteps: Seq[SearchableLearningStep],
                                  copyright: Copyright,
                                  isBasedOn: Option[Long])

