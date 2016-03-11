package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._

class LicenseValidatorTest extends UnitSuite with TestEnvironment {

  var validator: LicenseValidator = _
  override def beforeEach() = {
    validator = new LicenseValidator
  }

  test("Licensevalidator validates the license text"){
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(Some(ValidationMessage("path1.path2", "Invalid")))

    val validationMessage = validator.validate(Some(""))
    validationMessage.isDefined should be(right = true)
    validationMessage.get.field should equal("path1.path2")
    validationMessage.get.message should equal("Invalid")
  }

  test("That valid license doesn't give an error"){
    when(noHtmlTextValidator.validate(any[String], any[String])).thenReturn(None)
    validator.validate(Some("A random license")) should be (None)
  }

  test("That None doesn't give an error"){
    validator.validate(None) should be (None)
  }

}
