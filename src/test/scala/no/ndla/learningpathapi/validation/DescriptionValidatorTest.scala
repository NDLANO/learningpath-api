package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{Description, UnitSuite}

class DescriptionValidatorTest extends UnitSuite {

  test("That DescriptionValidator returns an error message for a description that is empty") {
    val validationErrors = DescriptionValidator.validate(Description("", None))

    validationErrors.head.field should equal("description.description")
    validationErrors.head.message should equal("Required value description is empty.")
  }

  test("That DescriptionValidator returns no error when description is not empty") {
    DescriptionValidator.validate(Description("Valid description", None)) should equal(List())
  }

  test("That DescriptionValidator returns error message for language") {
    val validationErrors = DescriptionValidator.validate(Description("Valid description", Some("unsupported")))

    validationErrors.head.field should equal("description.language")
    validationErrors.head.message should equal("Language 'unsupported' is not a supported value.")
  }

  test("That DescriptionValidator returns error message for both description and language") {
    val errorMessages = DescriptionValidator.validate(Description("", Some("unsupported")))
    errorMessages.size should be(2)

    errorMessages.head.field should equal("description.description")
    errorMessages.head.message should equal("Required value description is empty.")
    errorMessages.last.field should equal("description.language")
    errorMessages.last.message should equal("Language 'unsupported' is not a supported value.")
  }

  test("That DescriptionValidator returns no errors for a valid description") {
    DescriptionValidator.validate(Description("Valid description", Some("nb"))) should equal(List())
  }

  test("That DescriptionValidator.validate returns error message when no descriptions are defined") {
    val errorMessages = DescriptionValidator.validate(List())
    errorMessages.size should be(1)
    errorMessages.head.field should equal("description")
    errorMessages.head.message should equal("At least one description is required.")
  }

  test("That DescriptionValidator.validate returns error message for a list of descriptions, where one is invalid") {
    val descriptions = List(
      Description("Valid description", None),
      Description("", None))

    val errorMessages = DescriptionValidator.validate(descriptions)
    errorMessages.size should be(1)
    errorMessages.head.field should equal("description.description")
    errorMessages.head.message should equal("Required value description is empty.")
  }

  test("That DescriptionValidator.validate returns one error message per description that is invalid") {
    val descriptions = List(
      Description("", None),
      Description("", None),
      Description("", None))

    val errorMessages = DescriptionValidator.validate(descriptions)
    errorMessages.size should be(3)
    errorMessages.foreach(message => {
      message.field should equal("description.description")
      message.message should equal("Required value description is empty.")
    })
  }

  test("That DescriptionValidator.validate returns no errors when all descriptions are valid") {
    val descriptions = List(
      Description("Valid description in bokmål", Some("nb")),
      Description("Valid description in nynorsk", Some("nn")),
      Description("Valid description in english", Some("en"))
    )

    DescriptionValidator.validate(descriptions) should equal(List())
  }
}
