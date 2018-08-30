/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.CorrelationID
import no.ndla.learningpathapi.LearningpathApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import org.apache.logging.log4j.ThreadContext
import org.scalatra.CoreDsl

trait CorrelationIdSupport extends CoreDsl with LazyLogging {

  before() {
    CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
  }

  after() {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
  }

}
