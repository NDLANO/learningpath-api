/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import com.sksamuel.elastic4s.http.RequestFailure
import no.ndla.learningpathapi.model.api.ValidationMessage

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage])
    extends RuntimeException(message)
case class AccessDeniedException(message: String) extends RuntimeException(message)
class OptimisticLockException(message: String) extends RuntimeException(message)
class ImportException(message: String) extends RuntimeException(message)
case class NdlaSearchException(rf: RequestFailure)
    extends RuntimeException(
      s"""
     |index: ${rf.error.index.getOrElse("Error did not contain index")}
     |reason: ${rf.error.reason}
     |body: ${rf.body}
     |shard: ${rf.error.shard.getOrElse("Error did not contain shard")}
     |type: ${rf.error.`type`}
   """.stripMargin
    )
case class ElasticIndexingException(message: String) extends RuntimeException(message)
class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
case class LanguageNotSupportedException(message: String) extends RuntimeException(message)
case class InvalidStatusException(message: String) extends RuntimeException(message)
case class SearchException(message: String) extends RuntimeException(message)
case class NotFoundException(message: String) extends RuntimeException(message)
case class TaxonomyUpdateException(message: String) extends RuntimeException(message)
case class InvalidOembedResponse(message: String) extends RuntimeException(message)
