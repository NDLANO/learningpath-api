package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.domain._

class LearningStepValidatorTest extends UnitSuite {

  var validator: LearningStepValidator = _

  val warren = Author("author", "Goofy Elizabeth Warren")
  val license = License("Public Domain", "Public Domain", None)
  val copyright = Copyright(license, "", List(warren))
  val ValidLearningStep = LearningStep(id = None, revision = None, externalId = None, learningPathId = None, seqNo = 0,
    title = List(Title("Gyldig tittel", Some("nb"))),
    description = List(Description("<strong>Gyldig description</strong>", Some("nb"))),
    embedUrl = List(EmbedUrl("http://www.ndla.no/123", Some("nb"))),
    `type` = StepType.TEXT,
    copyright = Some(copyright),
    showTitle = true,
    status = StepStatus.ACTIVE
  )

  override def beforeEach() = {
    validator = new LearningStepValidator
  }

  test("That a valid learningstep does not give an error") {
    validator.validate(ValidLearningStep) should equal(List())
  }

  test("That validate returns error message when description contains illegal html") {
    val validationErrors = validator.validate(ValidLearningStep.copy(description = List(Description("<h1>Ugyldig</h1>", Some("nb")))))
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.description")
  }

  test("That validate returns error when description has an illegal language") {
    val validationErrors = validator.validate(ValidLearningStep.copy(description = List(Description("<strong>Gyldig beskrivelse</strong>", Some("bergensk")))))
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description.language")
  }

  test("That DescriptionValidator validates both description text and language") {
    val validationErrors = validator.validate(ValidLearningStep.copy(description = List(Description("<h1>Ugyldig</h1>", Some("bergensk")))))
    validationErrors.size should be(2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.language")
  }

  test("That validate returns error for all invalid descriptions") {
    val validationErrors = validator.validate(ValidLearningStep.copy(description = List(
      Description("<strong>Gyldig</strong>", Some("nb")),
      Description("<h1>Ugyldig</h1>", Some("nb")),
      Description("<h2>Også ugyldig</h2>", Some("nb"))
    )))

    validationErrors.size should be(2)
    validationErrors.head.field should equal("description.description")
    validationErrors.last.field should equal("description.description")
  }

  test("That validate returns error when embedContent contains html") {
    val validationMessages = validator.validate(ValidLearningStep.copy(embedUrl = List(EmbedUrl("<strong>ikke gyldig</strong>", Some("nb")))))
    validationMessages.size should be(1)
    validationMessages.head.field should equal("embedContent.url")
  }

  test("That validate returns error when embedContent.language is invalid") {
    val validationMessages = validator.validate(ValidLearningStep.copy(embedUrl = List(EmbedUrl("http://www.ndla.no/123", Some("bergensk")))))
    validationMessages.size should be(1)
    validationMessages.head.field should equal("embedContent.language")
  }

  test("That validate returns error for both embedContent.url and embedContent.language") {
    val validationMessages = validator.validate(ValidLearningStep.copy(embedUrl = List(EmbedUrl("<h1>Ugyldig</h1>", Some("bergensk")))))
    validationMessages.size should be(2)
    validationMessages.head.field should equal("embedContent.url")
    validationMessages.last.field should equal("embedContent.language")
  }

  test("That all embedContents are validated") {
    val validationMessages = validator.validate(ValidLearningStep.copy(embedUrl =
      List(
        EmbedUrl("<h1>Ugyldig</h1>", Some("nb")),
        EmbedUrl("http://www.ndla.no/123", Some("bergensk"))
      )))
    validationMessages.size should be(2)
    validationMessages.head.field should equal("embedContent.url")
    validationMessages.last.field should equal("embedContent.language")
  }

  test("That html-code in license returns an error") {
    val trump = Author("author", "Donald Drumpf")
    val license = License("<strong>ugyldig</strong>", "<strong>ugyldig</strong>", None)
    val copyright = Copyright(license, "", List(trump))
    val validationMessages = validator.validate(ValidLearningStep.copy(copyright = Some(copyright)))
    validationMessages.size should be(1)
    validationMessages.head.field should equal("license")
  }

  test("That None-license doesn't give an error") {
    validator.validate(ValidLearningStep.copy(copyright = None)) should equal(List())
  }

  test("That error is returned when no descriptions or embedContents are defined") {
    val validationErrors = validator.validate(ValidLearningStep.copy(description = List(), embedUrl = Seq()))
    validationErrors.size should be(1)
    validationErrors.head.field should equal("description|embedContent")
    validationErrors.head.message should equal("A learningstep is required to have either a description, embedContent or both.")
  }

  test("That no error is returned when a description is present, but no embedContents") {
    validator.validate(ValidLearningStep.copy(embedUrl = Seq())) should equal(Seq())
  }

  test("That no error is returned when an embedContent is present, but no descriptions") {
    validator.validate(ValidLearningStep.copy(description = List())) should equal(List())
  }
}
