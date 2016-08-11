/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, NativeSwaggerBase, Swagger}

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object LearningpathApiInfo {
  val apiInfo = ApiInfo(
    "Learningpath Api",
    "Documentation for the LEARNINGPATH API of NDLA.no",
    "http://ndla.no",
    LearningpathApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class LearningpathSwagger extends Swagger(Swagger.SpecVersion, "0.8", LearningpathApiInfo.apiInfo)
