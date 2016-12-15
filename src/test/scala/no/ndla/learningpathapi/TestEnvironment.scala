/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import javax.sql.DataSource

import io.searchbox.client.JestClient
import no.ndla.learningpathapi.controller.{HealthController, LearningpathController}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexBuilderServiceComponent, SearchIndexServiceComponent, SearchServiceComponent}
import no.ndla.learningpathapi.validation._
import no.ndla.network.NdlaClient
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
  extends LearningpathController
  with LearningPathRepositoryComponent
  with ReadServiceComponent
  with UpdateServiceComponent
  with SearchConverterServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with SearchIndexBuilderServiceComponent
  with NdlaClient
  with AuthClientComponent
  with ImageApiClientComponent
  with ConverterServiceComponent
  with ElasticClientComponent
  with DatasourceComponent
  with MockitoSugar
  with KeywordsServiceComponent
  with ImportServiceComponent
  with MigrationApiClient
  with Clock
  with HealthController
  with LanguageValidator
  with LearningPathValidator
  with LearningStepValidator
  with TitleValidator {

  val datasource = mock[DataSource]
  val searchIndexBuilderService = mock[SearchIndexBuilderService]

  val learningPathRepository = mock[LearningPathRepository]
  val readService = mock[ReadService]
  val updateService = mock[UpdateService]
  val searchConverterService = mock[SearchConverterService]
  val searchService = mock[SearchService]
  val searchIndexService = mock[SearchIndexService]
  val authClient = mock[AuthClient]
  val converterService = org.mockito.Mockito.spy(new ConverterService)
  val clock = mock[SystemClock]
  val ndlaClient = mock[NdlaClient]
  val imageApiClient = mock[ImageApiClient]
  val keywordsService = mock[KeywordsService]
  val migrationApiClient = mock[MigrationApiClient]
  val importService = mock[ImportService]
  val languageValidator = mock[LanguageValidator]
  val learningpathController = mock[LearningpathController]
  val healthController = mock[HealthController]
  val learningStepValidator = mock[LearningStepValidator]
  val learningPathValidator = mock[LearningPathValidator]
  val titleValidator = mock[TitleValidator]
  val jestClient = mock[JestClient]

  def resetMocks() = {
    Mockito.reset(
      datasource, searchIndexBuilderService, learningPathRepository, readService,
      updateService, searchService, searchIndexService, authClient, converterService, searchConverterService,
      languageValidator, titleValidator, jestClient
    )
  }
}
