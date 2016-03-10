package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi._
import org.mockito.Mockito._
import org.mockito.Matchers._

class NewLearningStepValidatorTest extends UnitSuite with TestEnvironment {

  val DefaultStep = NewLearningStep(List(Title("Some title", Some("nb"))), List(Description("Some description", Some("nb"))), List(EmbedContent("Some url", "<html-code>", Some("nb"))), "TEXT", Some("license"))

  var validator: NewLearningStepValidator = _
  override def beforeEach() = {
    validator = new NewLearningStepValidator
    when(titleValidator.validate(any[List[Title]])).thenReturn(List())
    when(basicHtmlDescriptionValidator.validateOptional(any[List[Description]])).thenReturn(List())
    when(embedContentValidator.validate(any[List[EmbedContent]])).thenReturn(List())
    when(stepTypeValidator.validate(any[String])).thenReturn(None)
    when(licenseValidator.validate(any[Option[String]])).thenReturn(None)
  }

  test("That error is returned when no descriptions or embedUrls are defined") {
    val validationErrors = validator.validate(DefaultStep.copy(description = List(), embedContent = List()))
    validationErrors.size should be (1)
    validationErrors.head.field should equal ("description|embedUrl")
    validationErrors.head.message should equal ("A learningstep is required to have either a description, embedUrl or both.")
  }

  test("That no error is returned when a description is present, but no embedUrls") {
    validator.validate(DefaultStep.copy(embedContent = List())) should equal (List())
  }

  test("That no error is returned when an embedUrl is present, but no descriptions") {
    validator.validate(DefaultStep.copy(description = List())) should equal (List())
  }

  test("That no error is returned when both description and embedUrls are present") {
    validator.validate(DefaultStep) should equal (List())
  }
}
