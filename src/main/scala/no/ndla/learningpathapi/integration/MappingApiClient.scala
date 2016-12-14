/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.caching.Memoize
import no.ndla.learningpathapi.model.api.License
import no.ndla.network.NdlaClient

import scala.util.{Failure, Success}
import scalaj.http.Http

trait MappingApiClient {
  this: NdlaClient =>
  val mappingApiClient: MappingApiClient

  class MappingApiClient {

    val allLanguageMappingsEndpoint = s"http://${LearningpathApiProperties.MappingHost}/iso639"
    val allLicenseDefinitionsEndpoint = s"http://${LearningpathApiProperties.MappingHost}/licenses"

    def getLicense(licenseName: String): Option[License] = {
      getLicenseDefinitions().find(_.license == licenseName).map(l => License(l.license, l.description, l.url))
    }

    def getLicenses(filter: Option[String]) : Seq[License] = {
      filter match {
        case Some(f) => getLicenseDefinitions().filter(_.license.startsWith(f)).map(l => License(l.license, l.description, l.url))
        case None => getLicenseDefinitions().map(l => License(l.license, l.description, l.url))
      }
    }

    def get6391CodeFor6392Code(languageCode6392: String): Option[String] = getLanguageMapping().find(_._1 == languageCode6392).map(_._2)

    def languageCodeSupported6391(languageCode: String): Boolean = getLanguageMapping().exists(_._2 == languageCode)

    def languageCodeSupported6392(languageCode: String): Boolean = getLanguageMapping().exists(_._1 == languageCode)

    private val getLicenseDefinitions = Memoize[Seq[LicenseDefinition]](LearningpathApiProperties.LicenseMappingCacheAgeInMs, () => {
      ndlaClient.fetch[Seq[LicenseDefinition]](Http(allLicenseDefinitionsEndpoint)) match {
        case Success(definitions) => definitions
        case Failure(ex) => throw ex
      }
    })

    private val getLanguageMapping = Memoize[Map[String, String]](LearningpathApiProperties.IsoMappingCacheAgeInMs, () => {
      ndlaClient.fetch[Map[String, String]](Http(allLanguageMappingsEndpoint)) match {
        case Success(map) => map
        case Failure(ex) => throw ex
      }
    })
  }
}

case class LicenseDefinition(license: String, description: Option[String], url: Option[String])