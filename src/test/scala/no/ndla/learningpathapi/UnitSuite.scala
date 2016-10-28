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
  val DEFAULT_PAGE_SIZE = 12
  val MAX_PAGE_SIZE = 548

  LearningpathApiProperties.setProperties(Map(
    "CONTACT_EMAIL" -> Some("ndla@knowit.no"),
    "HOST_ADDR" -> Some("localhost"),
    "NDLA_ENVIRONMENT" -> Some("local"),

    "DB_USER_NAME" -> Some("user"),
    "DB_PASSWORD" -> Some("password"),
    "DB_RESOURCE" -> Some("dbresource"),
    "DB_SERVER" -> Some("dbserver"),
    "DB_PORT" -> Some("1"),
    "DB_SCHEMA" -> Some("learningpathapi"),

    "SEARCH_SERVER" -> Some("search-server"),
    "RUN_WITH_SIGNED_SEARCH_REQUESTS" -> Some("false"),
    "SEARCH_REGION" -> Some("some-region"),
    "SEARCH_INDEX" -> Some("learningpaths"),
    "SEARCH_DOCUMENT" -> Some("learningpath"),
    "SEARCH_DEFAULT_PAGE_SIZE" -> Some(s"$DEFAULT_PAGE_SIZE"),
    "SEARCH_MAX_PAGE_SIZE" -> Some(s"$MAX_PAGE_SIZE"),
    "INDEX_BULK_SIZE" -> Some("500"),

    "MIGRATION_HOST" -> Some("migration-api"),
    "MIGRATION_USER" -> Some("username"),
    "MIGRATION_PASSWORD" -> Some("password")
  ))
}