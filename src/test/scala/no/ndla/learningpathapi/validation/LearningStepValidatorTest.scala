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
import org.mockito.Mockito._

class LearningStepValidatorTest extends UnitSuite with TestEnvironment {

  var validator: LearningStepValidator = _

  val license = "publicdomain"
  val ValidLearningStep = LearningStep(
    id = None,
    revision = None,
    externalId = None,
    learningPathId = None,
    seqNo = 0,
    title = List(Title("Gyldig tittel", "nb")),
    description = List(Description("<strong>Gyldig description</strong>", "nb")),
    embedUrl = List(EmbedUrl("https://www.ndla.no/123", "nb", EmbedType.OEmbed)),
    `type` = StepType.TEXT,
    license = Some(license),
    showTitle = true,
    status = StepStatus.ACTIVE
  )

  override def beforeEach() = {
    validator = new LearningStepValidator
    resetMocks()
  }
  private def validMock() = {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
  }

  test("That a valid learningstep does not give an error") {
    validMock()
    validator.validateLearningStep(ValidLearningStep, false) should equal(
      List())
  }

  test(
    "That validate returns error message when description contains illegal html") {
    validMock()
    val validationErrors = validator.validateLearningStep(
      ValidLearningStep.copy(
        description = List(Description("<h1>Ugyldig</h1>", "nb"))),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description")
  }

  test("That validate returns error when description has an illegal language") {
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    val validationErrors = validator.validateLearningStep(
      ValidLearningStep.copy(
        description =
          List(Description("<strong>Gyldig beskrivelse</strong>", "bergensk"))),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("language")
  }

  test("That DescriptionValidator validates both description text and language") {
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    val validationErrors = validator.validateLearningStep(
      ValidLearningStep.copy(
        description = List(Description("<h1>Ugyldig</h1>", "bergensk"))),
      false)
    validationErrors.size should be(2)
    validationErrors.head.field should equal("description")
    validationErrors.last.field should equal("language")
  }

  test("That validate returns error for all invalid descriptions") {
    validMock()
    val validationErrors = validator.validateLearningStep(
      ValidLearningStep.copy(
        description = List(
          Description("<strong>Gyldig</strong>", "nb"),
          Description("<h1>Ugyldig</h1>", "nb"),
          Description("<h2>Også ugyldig</h2>", "nb")
        )),
      false
    )

    validationErrors.size should be(2)
    validationErrors.head.field should equal("description")
    validationErrors.last.field should equal("description")
  }

  test("That validate returns error when embedUrl contains html") {
    validMock()
    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(embedUrl =
        List(EmbedUrl("<strong>ikke gyldig</strong>", "nb", EmbedType.OEmbed))),
      false)
    validationMessages.size should be(1)
    validationMessages.head.field should equal("embedUrl.url")
    validationMessages.head.message.contains("contains illegal html") should be(
      true)
  }

  test("That validate returns error when embedUrl.language is invalid") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))
    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(
        embedUrl = List(
          EmbedUrl("https://www.ndla.no/123", "bergensk", EmbedType.OEmbed))),
      false)
    validationMessages.size should be(1)
    validationMessages.head.field should equal("language")
  }

  test(
    "That validate returns error for both embedUrl.url and embedUrl.language") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))

    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(
        embedUrl =
          List(EmbedUrl("<h1>Ugyldig</h1>", "bergensk", EmbedType.OEmbed))),
      false)
    validationMessages.size should be(2)
    validationMessages.head.field should equal("embedUrl.url")
    validationMessages.last.field should equal("language")
  }

  test("That all embedUrls are validated") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())
    when(languageValidator.validate("language", "bergensk", false))
      .thenReturn(Some(ValidationMessage("language", "Error")))
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)

    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(
        embedUrl = List(
          EmbedUrl("<h1>Ugyldig</h1>", "nb", EmbedType.OEmbed),
          EmbedUrl("https://www.ndla.no/123", "bergensk", EmbedType.OEmbed)
        )),
      false
    )
    validationMessages.size should be(2)
    validationMessages.head.field should equal("embedUrl.url")
    validationMessages.last.field should equal("language")
  }

  test("Embedurls containing only paths should be legal") {
    when(languageValidator.validate("language", "nb", false)).thenReturn(None)
    when(titleValidator.validate(ValidLearningStep.title, false))
      .thenReturn(List())

    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(
        embedUrl = List(EmbedUrl(
          "/subjects/subject:9/topic:1:179373/topic:1:170165/resource:1:16145",
          "nb",
          EmbedType.OEmbed))),
      false)
    validationMessages.size should be(0)
  }

  test("That html-code in license returns an error") {
    validMock()
    val license = "<strong>ugyldig</strong>"
    val validationMessages = validator.validateLearningStep(
      ValidLearningStep.copy(license = Some(license)),
      false)
    validationMessages.size should be(1)
    validationMessages.head.field should equal("license")
  }

  test("That None-license doesn't give an error") {
    validMock()
    validator.validateLearningStep(ValidLearningStep.copy(license = None),
                                   false) should equal(List())
  }

  test("That error is returned when no descriptions or embedUrls are defined") {
    validMock()
    val validationErrors = validator.validateLearningStep(
      ValidLearningStep.copy(description = List(), embedUrl = Seq()),
      false)
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description|embedUrl")
    validationErrors.head.message should equal(
      "A learningstep is required to have either a description, embedUrl or both.")
  }

  test(
    "That no error is returned when a description is present, but no embedUrls") {
    validMock()
    validator.validateLearningStep(ValidLearningStep.copy(embedUrl = Seq()),
                                   false) should equal(Seq())
  }

  test(
    "That no error is returned when an embedUrl is present, but no descriptions") {
    validMock()
    validator.validateLearningStep(ValidLearningStep.copy(description = List()),
                                   false) should equal(List())
  }
}
