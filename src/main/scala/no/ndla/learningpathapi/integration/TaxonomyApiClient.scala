/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.ApiGatewayHost
import no.ndla.learningpathapi.model.domain.{TaxonomyUpdateException, Language, LearningPath, Title}
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import org.json4s.native.Serialization.write
import scalaj.http.Http
import cats.implicits._

import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxononyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats
    private val taxonomyTimeout = 20 * 1000 // 20 Seconds
    private val TaxonomyApiEndpoint = s"http://$ApiGatewayHost/taxonomy/v1"

    def updateTaxonomyIfExists(learningPath: LearningPath): Try[LearningPath] = {
      learningPath.id match {
        case None =>
          Failure(TaxonomyUpdateException("Can't update taxonomy resource when learningpath is missing id."))
        case Some(learningPathId) =>
          val contentUri = s"urn:learningpath:$learningPathId"

          Language.findByLanguageOrBestEffort(learningPath.title, Language.DefaultLanguage) match {
            case None =>
              Failure(TaxonomyUpdateException("Can't update taxonomy resource when learningpath is missing titles."))
            case Some(mainTitle) =>
              queryResource(contentUri) match {
                case Failure(ex) => Failure(ex)
                case Success(resources) =>
                  resources
                    .traverse(r => {
                      val resourceToPut = NewOrUpdateTaxonomyResource(
                        name = mainTitle.title,
                        contentUri = r.contentUri.getOrElse(contentUri)
                      )

                      updateTaxonomyResource(r.id, resourceToPut)
                        .flatMap(_ => updateResourceTranslations(r.id, learningPath.title))
                    })
                    .map(_ => learningPath)
              }
          }
      }
    }

    def updateTaxonomyResource(taxonomyId: String, resource: NewOrUpdateTaxonomyResource) = {
      putRaw[NewOrUpdateTaxonomyResource](s"$TaxonomyApiEndpoint/resources/${taxonomyId}", resource) match {
        case Failure(ex) =>
          logger.error(s"Failed updating taxonomy resource $taxonomyId with name.")
          Failure(ex)
        case Success(res) =>
          logger.info(s"Successfully updated $taxonomyId with name: '${resource.name}'...")
          Success(res)
      }
    }

    private def titleIsEqualToTranslation(title: Title, translation: Translation) =
      translation.name == title.title &&
        translation.language.exists(_ == title.language)

    private def updateResourceTranslations(resourceId: String, titles: Seq[Title]): Try[List[Translation]] = {
      getResourceTranslations(resourceId) match {
        case Failure(ex) =>
          logger.error(s"Failed to get translations for $resourceId when updating taxonomy...")
          Failure(ex)
        case Success(existingTranslations) =>
          val toDelete = existingTranslations.filterNot(_.language.exists(titles.map(_.language).contains))
          val deleted = toDelete.map(deleteResourceTranslation(resourceId, _))
          val updated = titles.toList.traverse(title =>
            existingTranslations.find(titleIsEqualToTranslation(title, _)) match {
              case Some(existingTranslation) => Success(existingTranslation)
              case None                      => updateResourceTranslation(resourceId, title.language, title.title)
          })

          deleted.collectFirst { case Failure(ex) => Failure(ex) } match {
            case Some(failedDelete) => failedDelete
            case None               => updated
          }
      }
    }

    private[integration] def updateResourceTranslation(resourceId: String, lang: String, name: String) =
      putRaw(s"$TaxonomyApiEndpoint/resources/$resourceId/translations/$lang", Translation(name))

    private[integration] def deleteResourceTranslation(resourceId: String, translation: Translation) = {
      translation.language
        .map(language => {
          delete(s"$TaxonomyApiEndpoint/resources/$resourceId/translations/$language")
        })
        .getOrElse({
          logger.info(s"Cannot delete translation without language for $resourceId")
          Success(())
        })
    }

    private[integration] def getResourceTranslations(resourceId: String) =
      get[List[Translation]](s"$TaxonomyApiEndpoint/resources/$resourceId/translations")

    private def queryResource(contentUri: String): Try[List[TaxonomyResource]] = {
      get[List[TaxonomyResource]](s"$TaxonomyApiEndpoint/queries/resources", "contentUri" -> contentUri) match {
        case Success(resources) => Success(resources)
        case Failure(ex)        => Failure(ex)
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

    private[integration] def delete(url: String, params: (String, String)*): Try[Unit] =
      ndlaClient.fetchRawWithForwardedAuth(
        Http(url).method("DELETE").timeout(taxonomyTimeout, taxonomyTimeout).params(params)) match {
        case Failure(ex) => Failure(ex)
        case Success(_)  => Success(())
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
