package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.LearningpathApiProperties.BasicHtmlTags
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class TextValidatorTest extends UnitSuite {

  var basicHtmlValidator: TextValidator = _
  var noHtmlValidator: TextValidator = _

  override def beforeEach() = {
    basicHtmlValidator = new TextValidator(allowHtml = true)
    noHtmlValidator = new TextValidator(allowHtml = false)
  }

  test("That TextValidator allows all tags in BasicHtmlTags tags") {
    BasicHtmlTags.foreach(tag => {
      val text = s"<$tag>This is text with $tag</$tag>"
      basicHtmlValidator.validate("path1.path2", text) should equal (None)
    })
  }

  test("That TextValidator does not allow tags outside BaiscHtmlTags") {
    val illegalTag = "a"
    BasicHtmlTags.contains(illegalTag) should be (right = false)

    val text = s"<$illegalTag>This is text with $illegalTag</$illegalTag>"

    val validationMessage = basicHtmlValidator.validate("path1.path2", text)
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal ("path1.path2")
    validationMessage.get.message should equal (basicHtmlValidator.IllegalContentInBasicText)
  }

  test("That TextValidator does not allow any html in plain text") {
    val textWithHtml = "<strong>This is text with html</strong>"
    val validationMessage = noHtmlValidator.validate("path1.path2", textWithHtml)
    validationMessage.isDefined should be (right = true)
    validationMessage.get.field should equal ("path1.path2")
    validationMessage.get.message should equal (noHtmlValidator.IllegalContentInPlainText)
  }

  test("That TextValidator allows plain text in plain text") {
    noHtmlValidator.validate("path1", "This is plain text") should be (None)
  }
}
