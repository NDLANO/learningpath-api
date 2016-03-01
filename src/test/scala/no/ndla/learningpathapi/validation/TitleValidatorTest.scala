package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.{Title, UnitSuite}

class TitleValidatorTest extends UnitSuite {

  test("That TitleValidator returns an error message for a title that is empty") {
    val validationErrors = TitleValidator.validate(Title("", None))

    validationErrors.head.field should equal("title.title")
    validationErrors.head.message should equal("Required value title is empty.")
  }

  test("That TitleValidator returns no error when title is not empty") {
    TitleValidator.validate(Title("Valid title", None)) should equal(List())
  }

  test("That TitleValidator returns error message for language") {
    val validationErrors = TitleValidator.validate(Title("Valid title", Some("unsupported")))

    validationErrors.head.field should equal("title.language")
    validationErrors.head.message should equal("Language 'unsupported' is not a supported value.")
  }

  test("That TitleValidator returns error message for both title and language") {
    val errorMessages = TitleValidator.validate(Title("", Some("unsupported")))
    errorMessages.size should be(2)

    errorMessages.head.field should equal("title.title")
    errorMessages.head.message should equal("Required value title is empty.")
    errorMessages.last.field should equal("title.language")
    errorMessages.last.message should equal("Language 'unsupported' is not a supported value.")
  }

  test("That TitleValidator returns no errors for a valid title") {
    TitleValidator.validate(Title("Valid title", Some("nb"))) should equal(List())
  }

  test("That TitleValidator.validate returns error message when no titles are defined") {
    val errorMessages = TitleValidator.validate(List())
    errorMessages.size should be(1)
    errorMessages.head.field should equal("title")
    errorMessages.head.message should equal("At least one title is required.")
  }

  test("That TitleValidator.validate returns error message for a list of titles, where one is invalid") {
    val titles = List(
      Title("Valid title", None),
      Title("", None))

    val errorMessages = TitleValidator.validate(titles)
    errorMessages.size should be(1)
    errorMessages.head.field should equal("title.title")
    errorMessages.head.message should equal("Required value title is empty.")
  }

  test("That TitleValidator.validate returns one error message per title that is invalid") {
    val titles = List(
      Title("", None),
      Title("", None),
      Title("", None))

    val errorMessages = TitleValidator.validate(titles)
    errorMessages.size should be(3)
    errorMessages.foreach(message => {
      message.field should equal("title.title")
      message.message should equal("Required value title is empty.")
    })
  }

  test("That TitleValidator.validate returns no errors when all titles are valid") {
    val titles = List(
      Title("Valid title in bokmål", Some("nb")),
      Title("Valid title in nynorsk", Some("nn")),
      Title("Valid title in english", Some("en"))
    )

    TitleValidator.validate(titles) should equal(List())
  }
}
