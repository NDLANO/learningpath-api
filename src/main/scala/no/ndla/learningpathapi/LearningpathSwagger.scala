/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object LearningpathApiInfo {

  val contactInfo: ContactInfo = ContactInfo(
    LearningpathApiProperties.ContactName,
    LearningpathApiProperties.ContactUrl,
    LearningpathApiProperties.ContactEmail
  )

  val licenseInfo: LicenseInfo = LicenseInfo(
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )

  val apiInfo: ApiInfo = ApiInfo(
    "Learningpath API",
    "Services for accessing learningpaths",
    LearningpathApiProperties.TermsUrl,
    contactInfo,
    licenseInfo
  )
}

class LearningpathSwagger extends Swagger("2.0", "1.0", LearningpathApiInfo.apiInfo) {
  addAuthorization(
    OAuth(List(), List(ImplicitGrant(LoginEndpoint(LearningpathApiProperties.Auth0LoginEndpoint), "access_token"))))
}
