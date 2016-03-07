package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{TestEnvironment, LearningpathApiProperties, UnitSuite}

class CoverPhotoValidatorTest extends UnitSuite with TestEnvironment {
  var validator: CoverPhotoValidator = _

  before {
    LearningpathApiProperties.setProperties(Map("DOMAINS" -> Some("api.ndla.no")))
  }

  override def beforeEach() = {
    validator = new CoverPhotoValidator
  }

  test("That validate returns no error for a valid url") {
    validator.validate(Some("http://api.ndla.no/images/full/sp2e9843.jpg")) should be(None)
  }

  test("That validate returns no error for no coverPhoto") {
    validator.validate(None) should be(None)
  }

  test("That validate returns an error when pointing some another api on ndla") {
    val validationError = validator.validate(Some("http://api.ndla.no/h5p/sp2e9843.jpg"))
    validationError.isDefined should be(right = true)
    validationError.get.field should equal("coverPhotoUrl")
    validationError.get.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test("That validate returns an error when pointing outside ndla") {
    val validationError = validator.validate(Some("http://www.vg.no/abc/123"))
    validationError.isDefined should be(right = true)
    validationError.get.field should equal("coverPhotoUrl")
    validationError.get.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }
}
