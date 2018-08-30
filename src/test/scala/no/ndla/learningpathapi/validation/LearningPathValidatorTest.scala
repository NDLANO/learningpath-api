/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.service.Clock
import org.mockito.Mockito._

class LearningPathValidatorTest
    extends UnitSuite
    with Clock
    with TestEnvironment {

  var validator: LearningPathValidator = _

  override val clock = new SystemClock

  override def beforeEach() = {
    validator = new LearningPathValidator
    resetMocks()

  }

  val trump = Author("author", "Donald Drumpf")
  val license = "pd"
  val copyright = Copyright(license, List(trump))
  val ValidLearningPath = LearningPath(
    id = None,
    title = List(Title("Gyldig tittel", "nb")),
    description = List(Description("Gyldig beskrivelse", "nb")),
    coverPhotoId = Some(s"http://api.ndla.no/image-api/v2/images/1"),
    duration = Some(180),
    tags = List(LearningPathTags(Seq("Gyldig tag"), "nb")),
    revision = None,
    externalId = None,
    isBasedOn = None,
    status = LearningPathStatus.PRIVATE,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    lastUpdated = clock.now(),
    owner = "",
    copyright = copyright
  )

  private def validMock() = {
    when(languageValidator.validate("description.language", "nb", false))
      .thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "nb", false))
      .thenReturn(None)
  }

  test("That valid learningpath returns no errors") {
    validMock()
    validator.validateLearningPath(ValidLearningPath,
                                   allowUnknownLanguage = false) should equal(
      List())
  }

  test("That validate returns no error for no coverPhoto") {
    validMock()
    validator.validateLearningPath(ValidLearningPath.copy(coverPhotoId = None),
                                   allowUnknownLanguage = false) should be(
      List())
  }

  test(
    "That validateCoverPhoto returns an error when metaUrl is pointing to some another api on ndla") {
    validMock()
    val validationError =
      validator.validateCoverPhoto(s"http://api.ndla.no/h5p/1")
    validationError.size should be(1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal(
      "The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test(
    "That validateCoverPhoto returns an error when metaUrl is pointing to empty string") {
    validMock()
    val validationError = validator.validateCoverPhoto("")
    validationError.size should be(1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal(
      "The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test(
    "That validateCoverPhoto returns an error when metaUrl is pointing to another Domain") {
    validMock()
    val validationError =
      validator.validateCoverPhoto("http://api.vg.no/images/1")
    validationError.size should be(1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal(
      "The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test("That validate returns error message when no descriptions are defined") {
    validMock()
    val errorMessages = validator.validateLearningPath(
      ValidLearningPath.copy(description = List()),
      allowUnknownLanguage = false)
    errorMessages.size should be(1)
    errorMessages.head.field should equal("description")
    errorMessages.head.message should equal(
      "At least one description is required.")
  }

  test(
    "That validate does not return error message when no descriptions are defined and no descriptions are required") {
    validMock()
    new LearningPathValidator(descriptionRequired = false)
      .validateLearningPath(ValidLearningPath.copy(description = List()),
                            allowUnknownLanguage = false) should equal(List())
  }

  test("That validate returns error message when description contains html") {
    validMock()
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        description = List(Description("<h1>Ugyldig</h1>", "nb"))),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.description")
  }
  test("That validate returns error when description has an illegal language") {
    when(languageValidator.validate("description.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "nb", false))
      .thenReturn(None)

    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        description = List(Description("Gyldig beskrivelse", "bergensk"))),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.language")
  }

  test(
    "That validate returns error message when description contains html even if description is not required") {
    validMock()
    val validationErrors = new LearningPathValidator(
      descriptionRequired = false).validateLearningPath(
      ValidLearningPath.copy(
        description = List(Description("<h1>Ugyldig</h1>", "nb"))),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.description")
  }

  test(
    "That validate returns error when description has an illegal language even if description is not required") {
    when(languageValidator.validate("description.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "nb", false))
      .thenReturn(None)

    val validationErrors = new LearningPathValidator(
      descriptionRequired = false).validateLearningPath(
      ValidLearningPath.copy(
        description = List(Description("Gyldig beskrivelse", "bergensk"))),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.language")
  }

  test("That DescriptionValidator validates both description text and language") {
    when(languageValidator.validate("description.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "nb", false))
      .thenReturn(None)

    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        description = List(Description("<h1>Ugyldig</h1>", "bergensk"))),
      false)
    validationErrors.size should be(2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.language")
  }

  test("That validate returns error for all invalid descriptions") {
    validMock()
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        description = List(
          Description("Gyldig", "nb"),
          Description("<h1>Ugyldig</h1>", "nb"),
          Description("<h2>Også ugyldig</h2>", "nb")
        )),
      false
    )

    validationErrors.size should be(2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.description")
  }

  test("That validate returns error when duration less than 1") {
    validMock()
    val validationError =
      validator.validateLearningPath(ValidLearningPath.copy(duration = Some(0)),
                                     false)
    validationError.size should be(1)
    validationError.head.field should equal("duration")
  }

  test("That validate accepts a learningpath without duration") {
    validMock()
    validator.validateLearningPath(ValidLearningPath.copy(duration = None),
                                   false) should equal(List())
  }

  test("That validate returns error when tag contains html") {
    validMock()
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        tags = List(LearningPathTags(Seq("<strong>ugyldig</strong>"), "nb"))),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("tags.tags")
  }

  test("That validate returns error when tag language is invalid") {
    when(languageValidator.validate("description.language", "nb", false))
      .thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("tags.language", "Error")))

    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        tags = List(LearningPathTags(Seq("Gyldig"), "bergensk"))),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("tags.language")
  }

  test("That returns error for both tag text and tag language") {
    when(languageValidator.validate("description.language", "nb", false))
      .thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title, false))
      .thenReturn(List())
    when(languageValidator.validate("tags.language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("tags.language", "Error")))

    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        tags =
          List(LearningPathTags(Seq("<strong>ugyldig</strong>"), "bergensk"))),
      false)
    validationErrors.size should be(2)
    validationErrors.head.field should equal("tags.tags")
    validationErrors.last.field should equal("tags.language")
  }

  test("That validate returns error for all invalid tags") {
    validMock()
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(
        tags = List(
          LearningPathTags(Seq("<strong>ugyldig</strong>",
                               "<li>også ugyldig</li>"),
                           "nb")
        )),
      false)
    validationErrors.size should be(2)
    validationErrors.head.field should equal("tags.tags")
    validationErrors.last.field should equal("tags.tags")
  }

  test("That validate returns error when copyright.license is invalid") {
    validMock()
    val invalidLicense = "dummy license"
    val invalidCopyright =
      ValidLearningPath.copyright.copy(license = invalidLicense)
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(copyright = invalidCopyright),
      false)

    validationErrors.size should be(1)
  }

  test("That validate returns no errors when license is valid") {
    validMock()
    validator.validateLearningPath(ValidLearningPath, false).isEmpty should be(
      true)
  }

  test("That validate returns error when copyright.contributors contains html") {
    validMock()
    val invalidCopyright = ValidLearningPath.copyright.copy(
      contributors = List(Author("<h1>wizardry</h1>", "<h1>Gandalf</h1>")))
    val validationErrors = validator.validateLearningPath(
      ValidLearningPath.copy(copyright = invalidCopyright),
      false)
    validationErrors.size should be(2)
  }

  test(
    "That validate returns no errors when copyright.contributors contains no html") {
    validMock()
    validator.validateLearningPath(ValidLearningPath, false).isEmpty should be(
      true)
  }
}
