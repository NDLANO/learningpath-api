/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain

import java.util.Date

case class Message(message: String, adminName: String, date: Date)
