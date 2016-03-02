package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.UnitSuite

class TextValidatorTest extends UnitSuite {

  test("That TextValidator allows all tags in BasicHtmlTags tags") {
    TextValidator.BasicHtmlTags.foreach(tag => {
      val text = s"<$tag>This is text with $tag</$tag>"
      TextValidator.validateOnlyBasicHtmlTags("path1.path2", text) should equal (None)
    })
  }

  test("That TextValidator does not allow tags outside BaiscHtmlTags") {
    val illegalTag = "a"
    TextValidator.BasicHtmlTags.contains(illegalTag) should be (right = false)

    val text = s"<$illegalTag>This is text with $illegalTag</$illegalTag>"

    val validationMessage = TextValidator.validateOnlyBasicHtmlTags("path1.path2", text)
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal ("path1.path2")
    validationMessage.get.message should equal (TextValidator.IllegalContentInBasicText)
  }

  test("That TextValidator does not allow any html in plain text") {
    val textWithHtml = "<strong>This is text with html</strong>"
    val validationMessage = TextValidator.validateNoHtmlTags("path1.path2", textWithHtml)
    validationMessage.isDefined should be (right = true)
    validationMessage.get.field should equal ("path1.path2")
    validationMessage.get.message should equal (TextValidator.IllegalContentInPlainText)
  }

  test("That TextValidator allows plain text in plain text") {
    TextValidator.validateNoHtmlTags("path1", "This is plain text") should be (None)
  }
}
