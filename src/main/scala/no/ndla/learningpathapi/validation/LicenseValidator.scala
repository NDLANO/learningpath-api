package no.ndla.learningpathapi.validation

import no.ndla.learningpathapi.ValidationMessage

object LicenseValidator {
  val LICENSE_EMPTY = "Required value license is empty."

  def validate(licenseOpt: Option[String]): Option[ValidationMessage] = {
    licenseOpt match {
      case None => None
      case Some(license) => {
        license.isEmpty match {
          case true => Some(ValidationMessage("license", LICENSE_EMPTY))
          case false => None
        }
      }
    }
  }

}
