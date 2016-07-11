package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.service.Clock

class LearningPathValidatorTest extends UnitSuite with Clock {

  var validator: LearningPathValidator = _
  val clock = new SystemClock

  override def beforeEach() = {
    validator = new LearningPathValidator

  }

  val trump = Author("author", "Donald Drumpf")
  val license = License("publicdomain")
  val copyright = Copyright(license, "Bag End", List(trump))
  val ValidLearningPath = LearningPath(
    id = None,
    title = List(Title("Gyldig tittel", Some("nb"))),
    description = List(Description("Gyldig beskrivelse", Some("nb"))),
    coverPhotoMetaUrl = Some("http://api.ndla.no/images/1"),
    duration = Some(180),
    tags = List(LearningPathTags(Seq("Gyldig tag"), Some("nb"))),
    revision = None,
    externalId = None,
    isBasedOn = None,
    status = LearningPathStatus.PRIVATE,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    lastUpdated = clock.now(),
    owner = "",
    copyright = copyright)

  test("That valid learningpath returns no errors") {
    validator.validate(ValidLearningPath) should equal (List())
  }

  test("That validate returns no error for no coverPhoto") {
    validator.validate(ValidLearningPath.copy(
      coverPhotoMetaUrl = None
    )) should be(List())
  }

  test("That validate returns an error when metaUrl is pointing to some another api on ndla") {
    val validationError = validator.validate(ValidLearningPath.copy(coverPhotoMetaUrl = Some("http://api.ndla.no/h5p/1")))
    validationError.size should be (1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }


  test("That validate returns error message when no descriptions are defined") {
    val errorMessages = validator.validate(ValidLearningPath.copy(description = List()))
    errorMessages.size should be (1)
    errorMessages.head.field should equal("description")
    errorMessages.head.message should equal("At least one description is required.")
  }

  test("That validate does not return error message when no descriptions are defined and no descriptions are required") {
    new LearningPathValidator(descriptionRequired = false).validate(ValidLearningPath.copy(description = List())) should equal (List())
  }

  test("That validate returns error message when description contains html") {
    val validationErrors = validator.validate(ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", Some("nb")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("description.description")
  }

  test("That validate returns error when description has an illegal language") {
    val validationErrors = validator.validate(ValidLearningPath.copy(description = List(Description("Gyldig beskrivelse", Some("bergensk")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("description.language")
  }

  test("That validate returns error message when description contains html even if description is not required") {
    val validationErrors = new LearningPathValidator(descriptionRequired = false).validate(ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", Some("nb")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("description.description")
  }

  test("That validate returns error when description has an illegal language even if description is not required") {
    val validationErrors = new LearningPathValidator(descriptionRequired = false).validate(ValidLearningPath.copy(description = List(Description("Gyldig beskrivelse", Some("bergensk")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("description.language")
  }

  test("That DescriptionValidator validates both description text and language") {
    val validationErrors = validator.validate(ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", Some("bergensk")))))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.language")
  }

  test("That validate returns error for all invalid descriptions") {
    val validationErrors = validator.validate(ValidLearningPath.copy(description = List(
      Description("Gyldig", Some("nb")),
      Description("<h1>Ugyldig</h1>", Some("nb")),
      Description("<h2>Også ugyldig</h2>", Some("nb"))
    )))

    validationErrors.size should be (2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.description")
  }

  test("That validate returns error when duration less than 1") {
    val validationError = validator.validate(ValidLearningPath.copy(duration = Some(0)))
    validationError.size should be (1)
    validationError.head.field should equal("duration")
  }

  test("That validate accepts a learningpath without duration") {
    validator.validate(ValidLearningPath.copy(duration = None)) should equal (List())
  }

  test("That validate returns error when tag contains html") {
    val validationErrors = validator.validate(ValidLearningPath.copy(tags = List(LearningPathTags(Seq("<strong>ugyldig</strong>"), Some("nb")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("tags.tags")
  }

  test("That validate returns error when tag language is invalid") {
    val validationErrors = validator.validate(ValidLearningPath.copy(tags = List(LearningPathTags(Seq("Gyldig"), Some("bergensk")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("tags.language")
  }

  test("That returns error for both tag text and tag language") {
    val validationErrors = validator.validate(ValidLearningPath.copy(tags = List(LearningPathTags(Seq("<strong>ugyldig</strong>"), Some("bergensk")))))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("tags.tags")
    validationErrors.last.field should equal("tags.language")
  }

  test("That validate returns error for all invalid tags") {
    val validationErrors = validator.validate(ValidLearningPath.copy(tags = List(
      LearningPathTags(Seq("<strong>ugyldig</strong>", "<li>også ugyldig</li>"), Some("nb"))
    )))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("tags.tags")
    validationErrors.last.field should equal("tags.tags")
  }

  test("That validate returns error when copyright.license is invalid") {
    val invalidLicense = License("dummy license")
    val invalidCopyright = ValidLearningPath.copyright.copy(license = invalidLicense)
    val validationErrors = validator.validate(ValidLearningPath.copy(copyright = invalidCopyright))

    validationErrors.size should be (1)
  }

  test("That validate returns no errors when license is valid") {
    validator.validate(ValidLearningPath).isEmpty should be (true)
  }

  test("That validate returns error when copyright.contributors contains html") {
    val invalidCopyright = ValidLearningPath.copyright.copy(contributors = List(Author("<h1>wizardry</h1>", "<h1>Gandalf</h1>")))
    val validationErrors = validator.validate(ValidLearningPath.copy(copyright = invalidCopyright))

    validationErrors.size should be (2)
  }

  test("That validate returns no errors when copyright.contributors contains no html") {
    validator.validate(ValidLearningPath).isEmpty should be (true)
  }

  test("That validate returns error when copyright.origin contains html") {
    val invalidCopyright = ValidLearningPath.copyright.copy(origin = "<h1>Mordor</h1>")
    val validationErrors = validator.validate(ValidLearningPath.copy(copyright = invalidCopyright))

    validationErrors.size should be (1)
  }

  test("That validate returns no error when copyright.origin contains no html") {
    validator.validate(ValidLearningPath).isEmpty should be (true)
  }
}

