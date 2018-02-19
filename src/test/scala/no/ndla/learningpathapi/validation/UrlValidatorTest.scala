/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite

class UrlValidatorTest extends UnitSuite {

  var validator: UrlValidator = _

  override def beforeEach() = {
    validator = new UrlValidator
  }

  val fieldname = "a.field"

  test("That empty url returns a ValidationMessage") {
    val validationMessages = validator.validate(fieldname, "")
    validationMessages.size should be (1)
    validationMessages.head.field should equal(fieldname)
    validationMessages.head.message should equal("Required field is empty.")
  }

  test("That html in url returns a ValidationMessage") {
    val validationMessages = validator.validate(fieldname, "https://<h1>")
    validationMessages.size should be (1)
    validationMessages.head.field should equal(fieldname)
    validationMessages.head.message should equal("The content contains illegal html-characters. No HTML is allowed.")
  }

  test("That non-https in url returns a ValidationMessage") {
    val validationMessages = validator.validate(fieldname, "http://something.no")
    validationMessages.size should be (1)
    validationMessages.head.field should equal(fieldname)
    validationMessages.head.message should equal("Illegal Url. All Urls must start with https.")
  }

  test("That https in url is ok") {
    val validationMessages = validator.validate(fieldname, "https://something.no")
    validationMessages.size should be (0)
  }
}
