/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.ApiGatewayHost
import no.ndla.learningpathapi.model.domain.{InvalidLearningPathException, Language, LearningPath, Title}
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import org.json4s.native.Serialization.write
import scalaj.http.Http

import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxononyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats
    private val taxonomyTimeout = 20 * 1000 // 20 Seconds
    private val TaxonomyApiEndpoint = s"http://$ApiGatewayHost/taxonomy/v1"

    def updateName(learningPath: LearningPath, name: String): Try[LearningPath] = {
      learningPath.id match {
        case None =>
          Failure(InvalidLearningPathException("Can't update taxonomy resource when learningpath is missing id."))
        case Some(learningPathId) =>
          val contentUri = s"urn:learningpath:$learningPathId"

          Language.findByLanguageOrBestEffort(learningPath.title, Language.DefaultLanguage) match {
            case None =>
              Failure(
                InvalidLearningPathException("Can't update taxonomy resource when learningpath is missing titles."))
            case Some(mainTitle) =>
              queryResource(contentUri) match {
                case Failure(ex) => Failure(ex)
                case Success(resources) =>
                  resources.map(r => {
                    val resourceToPut = NewOrUpdateTaxonomyResource(
                      name = mainTitle.title,
                      contentUri = r.contentUri.getOrElse(contentUri)
                    )

                    updateTaxonomyResource(r.id, resourceToPut, learningPath.title)
                  })
                  ??? // TODO: Return type
              }
          }
      }
    }

    def updateTaxonomyResource(taxonomyId: String, resource: NewOrUpdateTaxonomyResource, titles: Seq[Title]) = {
      putRaw[NewOrUpdateTaxonomyResource](s"$TaxonomyApiEndpoint/resources/${taxonomyId}", resource) match {
        case Failure(ex) =>
          logger.error(s"Failed updating taxonomy resource $taxonomyId with name.")
          Failure(ex)
        case Success(_) =>
          logger.info(s"Successfully updated $taxonomyId with name: '${resource.name}'...")
          updateResourceTranslations(taxonomyId, titles)
      }
    }

    def updateResourceTranslations(resourceId: String, titles: Seq[Title]): Try[_] = { // TODO:
      getResourceTranslations(resourceId) match {
        case Failure(ex) =>
          logger.error(s"Failed to get translations for $resourceId when updating taxonomy...")
          Failure(ex)
        case Success(existingTranslations) =>
          titles.map(title => {
            val translation = Translation(name = title.title)
            // TODO: Compare translations and delete + put

          })
      }

    }

    private[integration] def getResourceTranslations(resourceId: String) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/resources/$resourceId/translations")

    private def queryResource(contentUri: String): Try[List[TaxonomyResource]] = {
      get[TaxonomyResource](s"$TaxonomyApiEndpoint/queries/resources", "contentUri" -> contentUri) match {
        case Success(resource)                             => Success(Some(resource))
        case Failure(ex: HttpRequestException) if ex.is404 => Success(None)
        case Failure(ex)                                   => Failure(ex)
      }
    }

    def getResource(nodeId: String): Try[TaxonomyResource] = {
      val resourceId = s"urn:resource:1:$nodeId"
      get[TaxonomyResource](s"$TaxonomyApiEndpoint/resources/$resourceId") match {
        case Failure(ex) =>
          Failure(ex)
        case Success(a) =>
          Success(a)
      }
    }

    def updateResource(resource: TaxonomyResource): Try[TaxonomyResource] = {
      put[String, TaxonomyResource](s"$TaxonomyApiEndpoint/resources/${resource.id}", resource) match {
        case Success(_) => Success(resource)
        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.is2xx) =>
          Success(resource)
        case Failure(ex) => Failure(ex)
      }
    }

    private def get[A](url: String, params: (String, String)*)(implicit mf: Manifest[A]): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](Http(url).timeout(taxonomyTimeout, taxonomyTimeout).params(params))
    }

    private def put[A, B <: AnyRef](url: String, data: B, params: (String, String)*)(
        implicit mf: Manifest[A],
        format: org.json4s.Formats): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        Http(url)
          .timeout(taxonomyTimeout, taxonomyTimeout)
          .postData(write(data))
          .method("put")
          .header("content-type", "application/json")
          .params(params.toMap)
      )
    }

    private def post[A, B <: AnyRef](url: String, data: B, params: (String, String)*)(
        implicit mf: Manifest[A],
        format: org.json4s.Formats): Try[A] = {
      ndlaClient.fetchWithForwardedAuth[A](
        Http(url)
          .timeout(taxonomyTimeout, taxonomyTimeout)
          .postData(write(data))
          .header("content-type", "application/json")
          .params(params.toMap)
      )
    }

    private[integration] def putRaw[B <: AnyRef](url: String, data: B, params: (String, String)*)(
        implicit formats: org.json4s.Formats): Try[B] = {
      logger.info(s"Doing call to $url")
      ndlaClient.fetchRawWithForwardedAuth(
        Http(url)
          .put(write(data))
          .timeout(taxonomyTimeout, taxonomyTimeout)
          .header("content-type", "application/json")
          .params(params)
      ) match {
        case Success(_)  => Success(data)
        case Failure(ex) => Failure(ex)
      }
    }

  }
}

case class Translation(name: String, language: Option[String] = None)

case class NewOrUpdateTaxonomyResource(
    name: String,
    contentUri: String
)

case class TaxonomyResource(
    id: String,
    name: String,
    contentUri: Option[String],
    path: String,
)
