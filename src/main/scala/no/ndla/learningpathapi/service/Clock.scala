/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.util.Date


trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): Date = {
      new Date()
    }
  }
}

