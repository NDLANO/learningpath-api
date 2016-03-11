package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.model.api.ValidationMessage

trait LicenseValidatorComponent {
  this: TextValidatorComponent =>
  val licenseValidator: LicenseValidator

  class LicenseValidator {
    def validate(licenseOpt: Option[String]): Option[ValidationMessage] = {
      licenseOpt match {
        case None => None
        case Some(license) => noHtmlTextValidator.validate("license", license)
      }
    }
  }
}
