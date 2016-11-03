/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {


  LearningpathApiProperties.setProperties(Map(
    "CONTACT_EMAIL" -> Some("ndla@knowit.no"),
    "HOST_ADDR" -> Some("localhost"),
    "DOMAIN" -> Some("http://localhost"),

    "META_USER_NAME" -> Some("user"),
    "META_PASSWORD" -> Some("password"),
    "META_RESOURCE" -> Some("dbresource"),
    "META_SERVER" -> Some("dbserver"),
    "META_PORT" -> Some("1"),
    "META_SCHEMA" -> Some("learningpathapi"),

    "SEARCH_SERVER" -> Some("search-server"),
    "RUN_WITH_SIGNED_SEARCH_REQUESTS" -> Some("false"),
    "SEARCH_REGION" -> Some("some-region"),

    "MIGRATION_HOST" -> Some("migration-api"),
    "MIGRATION_USER" -> Some("username"),
    "MIGRATION_PASSWORD" -> Some("password")
  ))
}