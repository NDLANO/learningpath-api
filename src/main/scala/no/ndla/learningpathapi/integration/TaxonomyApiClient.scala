/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.integration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.LearningpathApiProperties.ApiGatewayHost
import no.ndla.learningpathapi.model.domain.{Language, LearningPath, TaxonomyUpdateException, Title}
import no.ndla.network.NdlaClient
import no.ndla.network.model.HttpRequestException
import org.json4s.native.Serialization.write
import scalaj.http.{Http, HttpResponse}
import cats.implicits._

import scala.util.{Failure, Success, Try}

trait TaxonomyApiClient {
  this: NdlaClient =>
  val taxononyApiClient: TaxonomyApiClient

  class TaxonomyApiClient extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats
    private val taxonomyTimeout = 20 * 1000 // 20 Seconds
    private val TaxonomyApiEndpoint = s"http://$ApiGatewayHost/taxonomy/v1"
    private val LearningPathResourceTypeId = "urn:resourcetype:learningPath"

    def updateTaxonomyForLearningPath(learningPath: LearningPath,
                                      createResourceIfMissing: Boolean): Try[LearningPath] = {
      val result = learningPath.id match {
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
                case Success(resources) if resources.isEmpty && createResourceIfMissing =>
                  createAndUpdateResource(learningPath, contentUri, mainTitle)
                case Success(resources) =>
                  updateExistingResources(resources, contentUri, learningPath.title, mainTitle)
              }
          }
      }
      result.map(_ => learningPath)
    }

    private def createAndUpdateResource(learningPath: LearningPath, contentUri: String, mainTitle: Title) = {
      val newResource = NewOrUpdateTaxonomyResource(
        name = mainTitle.title,
        contentUri = contentUri
      )
      createResource(newResource) match {
        case Failure(ex) => Failure(ex)
        case Success(newLocation) =>
          newLocation.split('/').lastOption match {
            case None =>
              val msg = "Wasn't able to derive id from taxonomy create response, this is probably a bug."
              logger.error(msg)
              Failure(TaxonomyUpdateException(msg))
            case Some(resourceId) =>
              val newResource = TaxonomyResource(
                id = resourceId,
                name = mainTitle.title,
                contentUri = Some(contentUri),
                path = None
              )
              addLearningPathResourceType(resourceId).flatMap(_ =>
                updateExistingResources(List(newResource), contentUri, learningPath.title, mainTitle))
          }
      }
    }

    private def addLearningPathResourceType(resourceId: String): Try[String] = {
      val resourceType = ResourceResourceType(
        resourceId = resourceId,
        resourceTypeId = LearningPathResourceTypeId
      )
      postRaw[ResourceResourceType](s"$TaxonomyApiEndpoint/resource-resourcetypes", resourceType) match {
        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.is2xx) => Success(resourceId)
        case Failure(ex)                                                          => Failure(ex)
        case Success(_)                                                           => Success(resourceId)
      }
    }

    private def updateTaxonomyResource(taxonomyId: String, resource: NewOrUpdateTaxonomyResource) = {
      putRaw[NewOrUpdateTaxonomyResource](s"$TaxonomyApiEndpoint/resources/${taxonomyId}", resource) match {
        case Failure(ex) =>
          logger.error(s"Failed updating taxonomy resource $taxonomyId with name.")
          Failure(ex)
        case Success(res) =>
          logger.info(s"Successfully updated $taxonomyId with name: '${resource.name}'...")
          Success(res)
      }
    }

    private def createResource(resource: NewOrUpdateTaxonomyResource) = {
      postRaw[NewOrUpdateTaxonomyResource](s"$TaxonomyApiEndpoint/resources", resource) match {
        case Success(resp) =>
          resp.header("location") match {
            case Some(locationHeader) if locationHeader.nonEmpty => Success(locationHeader)
            case _                                               => Failure(new TaxonomyUpdateException("Could not get location after inserting resource"))
          }

        case Failure(ex: HttpRequestException) if ex.httpResponse.exists(_.is2xx) =>
          ex.httpResponse.flatMap(_.header("location")) match {
            case Some(locationHeader) if locationHeader.nonEmpty => Success(locationHeader)
            case _                                               => Failure(ex)
          }
        case Failure(ex) => Failure(ex)
      }
    }

    private def updateExistingResources(existingResources: List[TaxonomyResource],
                                        contentUri: String,
                                        titles: Seq[Title],
                                        mainTitle: Title) = {
      existingResources
        .traverse(r => {
          val resourceToPut = NewOrUpdateTaxonomyResource(
            name = mainTitle.title,
            contentUri = r.contentUri.getOrElse(contentUri)
          )

          updateTaxonomyResource(r.id, resourceToPut)
            .flatMap(_ => updateResourceTranslations(r.id, titles))
        })
    }

    private def titleIsEqualToTranslation(title: Title, translation: Translation) =
      translation.name == title.title &&
        translation.language.exists(_ == title.language)

    private def updateResourceTranslations(resourceId: String, titles: Seq[Title]): Try[List[Translation]] = {
      // Since 'unknown' language is known as 'unk' in taxonomy we do a conversion
      val titlesWithConvertedLang = titles.map(t => t.copy(language = t.language.replace("unknown", "unk")))
      getResourceTranslations(resourceId) match {
        case Failure(ex) =>
          logger.error(s"Failed to get translations for $resourceId when updating taxonomy...")
          Failure(ex)
        case Success(existingTranslations) =>
          val toDelete =
            existingTranslations.filterNot(_.language.exists(titlesWithConvertedLang.map(_.language).contains))
          val deleted = toDelete.map(deleteResourceTranslation(resourceId, _))
          val updated = titlesWithConvertedLang.toList.traverse(title =>
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
      get[List[TaxonomyResource]](s"$TaxonomyApiEndpoint/queries/resources", "contentURI" -> contentUri) match {
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

    def queryResource(articleId: Long): Try[List[Resource]] =
      get[List[Resource]](s"$TaxonomyApiEndpoint/queries/resources", "contentURI" -> s"urn:article:$articleId")

    def queryTopic(articleId: Long): Try[List[Topic]] =
      get[List[Topic]](s"$TaxonomyApiEndpoint/queries/topics", "contentURI" -> s"urn:article:$articleId")

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

    private def postRaw[B <: AnyRef](endpointUrl: String, data: B, params: (String, String)*)(
        implicit format: org.json4s.Formats): Try[HttpResponse[String]] = {
      ndlaClient.fetchRawWithForwardedAuth(
        Http(endpointUrl)
          .postData(write(data))
          .timeout(taxonomyTimeout, taxonomyTimeout)
          .header("content-type", "application/json")
          .params(params)
      ) match {
        case Success(resp) => Success(resp)
        case Failure(ex)   => Failure(ex)
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
    path: Option[String],
)

case class ResourceResourceType(
    resourceId: String,
    resourceTypeId: String
)

trait Taxonomy[E <: Taxonomy[E]] {
  val id: String
  def name: String
  def withName(name: String): E
}

case class Resource(id: String, name: String, contentUri: Option[String], paths: List[String])
    extends Taxonomy[Resource] {
  def withName(name: String): Resource = this.copy(name = name)
}
case class Topic(id: String, name: String, contentUri: Option[String], paths: List[String]) extends Taxonomy[Topic] {
  def withName(name: String): Topic = this.copy(name = name)
}
