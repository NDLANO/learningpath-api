package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.service.Clock
import org.mockito.Mockito._

class LearningPathValidatorTest extends UnitSuite with Clock with TestEnvironment {

  var validator: LearningPathValidator = _

  override val clock = new SystemClock

  override def beforeEach() = {
    validator = new LearningPathValidator
    resetMocks()

  }

  val trump = Author("author", "Donald Drumpf")
  val license = "publicdomain"
  val copyright = Copyright(license, List(trump))
  val ValidLearningPath = LearningPath(
    id = None,
    title = List(Title("Gyldig tittel", Some("nb"))),
    description = List(Description("Gyldig beskrivelse", Some("nb"))),
    coverPhotoMetaUrl = Some(s"${LearningpathApiProperties.Domain}/images/1"),
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

  private def validMock() = {
    when(languageValidator.validate("description.language", Some("nb"))).thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title)).thenReturn(List())
    when(languageValidator.validate("tags.language", Some("nb"))).thenReturn(None)
  }
  test("That valid learningpath returns no errors") {
    validMock()
    validator.validateMessages(ValidLearningPath) should equal (List())
  }

  test("That validate returns no error for no coverPhoto") {
    validMock()
    validator.validateMessages(ValidLearningPath.copy(
      coverPhotoMetaUrl = None
    )) should be(List())
  }

  test("That validate returns an error when metaUrl is pointing to some another api on ndla") {
    validMock()
    val validationError = validator.validateMessages(ValidLearningPath.copy(coverPhotoMetaUrl = Some(s"${LearningpathApiProperties.Domain}/h5p/1")))
    validationError.size should be (1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test("That validate returns an error when metaUrl is pointing to empty string") {
    validMock()
    val validationError = validator.validateMessages(ValidLearningPath.copy(coverPhotoMetaUrl = Some("")))
    validationError.size should be (1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }

  test("That validate returns an error when metaUrl is pointing to another Domain") {
    validMock()
    val validationError = validator.validateMessages(ValidLearningPath.copy(coverPhotoMetaUrl = Some("http://api.supertest.ndla.no/images/1")))
    validationError.size should be (1)
    validationError.head.field should equal("coverPhotoMetaUrl")
    validationError.head.message should equal("The url to the coverPhoto must point to an image in NDLA Image API.")
  }


  test("That validate returns error message when no descriptions are defined") {
    validMock()
    val errorMessages = validator.validateMessages(ValidLearningPath.copy(description = List()))
    errorMessages.size should be (1)
    errorMessages.head.field should equal("description")
    errorMessages.head.message should equal("At least one description is required.")
  }

  test("That validate does not return error message when no descriptions are defined and no descriptions are required") {
    validMock()
    new LearningPathValidator(descriptionRequired = false).validateMessages(ValidLearningPath.copy(description = List())) should equal (List())
  }

  test("That validate returns error message when description contains html") {
    validMock()
    val validationErrors = validator.validateMessages(ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", Some("nb")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("description.description")
  }
  test("That validate returns error when description has an illegal language") {
    when(languageValidator.validate("description.language", Some("bergensk"))).thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title)).thenReturn(List())
    when(languageValidator.validate("tags.language", Some("nb"))).thenReturn(None)

    val validationErrors = validator.validateMessages(ValidLearningPath.copy(description = List(Description("Gyldig beskrivelse", Some("bergensk")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("description.language")
  }

  test("That validate returns error message when description contains html even if description is not required") {
    validMock()
    val validationErrors = new LearningPathValidator(descriptionRequired = false).validateMessages(ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", Some("nb")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("description.description")
  }

  test("That validate returns error when description has an illegal language even if description is not required") {
    when(languageValidator.validate("description.language", Some("bergensk"))).thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title)).thenReturn(List())
    when(languageValidator.validate("tags.language", Some("nb"))).thenReturn(None)

    val validationErrors = new LearningPathValidator(descriptionRequired = false).validateMessages(ValidLearningPath.copy(description = List(Description("Gyldig beskrivelse", Some("bergensk")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("description.language")
  }

  test("That DescriptionValidator validates both description text and language") {
    when(languageValidator.validate("description.language", Some("bergensk"))).thenReturn(Some(ValidationMessage("description.language", "Error")))
    when(titleValidator.validate(ValidLearningPath.title)).thenReturn(List())
    when(languageValidator.validate("tags.language", Some("nb"))).thenReturn(None)

    val validationErrors = validator.validateMessages(ValidLearningPath.copy(description = List(Description("<h1>Ugyldig</h1>", Some("bergensk")))))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.language")
  }

  test("That validate returns error for all invalid descriptions") {
    validMock()
    val validationErrors = validator.validateMessages(ValidLearningPath.copy(description = List(
      Description("Gyldig", Some("nb")),
      Description("<h1>Ugyldig</h1>", Some("nb")),
      Description("<h2>Også ugyldig</h2>", Some("nb"))
    )))

    validationErrors.size should be (2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.description")
  }

  test("That validate returns error when duration less than 1") {
    validMock()
    val validationError = validator.validateMessages(ValidLearningPath.copy(duration = Some(0)))
    validationError.size should be (1)
    validationError.head.field should equal("duration")
  }

  test("That validate accepts a learningpath without duration") {
    validMock()
    validator.validateMessages(ValidLearningPath.copy(duration = None)) should equal (List())
  }

  test("That validate returns error when tag contains html") {
    validMock()
    val validationErrors = validator.validateMessages(ValidLearningPath.copy(tags = List(LearningPathTags(Seq("<strong>ugyldig</strong>"), Some("nb")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("tags.tags")
  }

  test("That validate returns error when tag language is invalid") {
    when(languageValidator.validate("description.language", Some("nb"))).thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title)).thenReturn(List())
    when(languageValidator.validate("tags.language", Some("bergensk"))).thenReturn(Some(ValidationMessage("tags.language", "Error")))

    val validationErrors = validator.validateMessages(ValidLearningPath.copy(tags = List(LearningPathTags(Seq("Gyldig"), Some("bergensk")))))
    validationErrors.size should be (1)
    validationErrors.head.field should equal("tags.language")
  }

  test("That returns error for both tag text and tag language") {
    when(languageValidator.validate("description.language", Some("nb"))).thenReturn(None)
    when(titleValidator.validate(ValidLearningPath.title)).thenReturn(List())
    when(languageValidator.validate("tags.language", Some("bergensk"))).thenReturn(Some(ValidationMessage("tags.language", "Error")))

    val validationErrors = validator.validateMessages(ValidLearningPath.copy(tags = List(LearningPathTags(Seq("<strong>ugyldig</strong>"), Some("bergensk")))))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("tags.tags")
    validationErrors.last.field should equal("tags.language")
  }

  test("That validate returns error for all invalid tags") {
    validMock()
    val validationErrors = validator.validateMessages(ValidLearningPath.copy(tags = List(
      LearningPathTags(Seq("<strong>ugyldig</strong>", "<li>også ugyldig</li>"), Some("nb"))
    )))
    validationErrors.size should be (2)
    validationErrors.head.field should equal("tags.tags")
    validationErrors.last.field should equal("tags.tags")
  }

  test("That validate returns error when copyright.license is invalid") {
    validMock()
    val invalidLicense = "dummy license"
    val invalidCopyright = ValidLearningPath.copyright.copy(license = invalidLicense)
    when(mappingApiClient.getLicenseDefinition(invalidLicense)).thenReturn(None)
    val validationErrors = validator.validateMessages(ValidLearningPath.copy(copyright = invalidCopyright))

    validationErrors.size should be (1)
  }

  test("That validate returns no errors when license is valid") {
    validMock()
    validator.validateMessages(ValidLearningPath).isEmpty should be (true)
  }

  test("That validate returns error when copyright.contributors contains html") {
    validMock()
    val invalidCopyright = ValidLearningPath.copyright.copy(contributors = List(Author("<h1>wizardry</h1>", "<h1>Gandalf</h1>")))
    val validationErrors = validator.validateMessages(ValidLearningPath.copy(copyright = invalidCopyright))
    validationErrors.size should be (2)
  }

  test("That validate returns no errors when copyright.contributors contains no html") {
    validMock()
    validator.validateMessages(ValidLearningPath).isEmpty should be (true)
  }
}

