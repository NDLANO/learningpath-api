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
import no.ndla.learningpathapi.controller.{HealthController, LearningpathController, LearningpathControllerV2}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexServiceComponent, SearchServiceComponent}
import no.ndla.learningpathapi.validation._
import no.ndla.network.NdlaClient
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
  extends LearningpathController
  with LearningpathControllerV2
  with LearningPathRepositoryComponent
  with ReadServiceComponent
  with UpdateServiceComponent
  with SearchConverterServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with NdlaClient
  with ImageApiClientComponent
  with ArticleApiClient
  with MigrationApiClient
  with ConverterServiceComponent
  with ElasticClientComponent
  with DatasourceComponent
  with MockitoSugar
  with KeywordsServiceComponent
  with ImportServiceComponent
  with Clock
  with HealthController
  with LanguageValidator
  with LearningPathValidator
  with LearningStepValidator
  with TitleValidator {

  val datasource = mock[DataSource]

  val learningPathRepository = mock[LearningPathRepository]
  val readService = mock[ReadService]
  val updateService = mock[UpdateService]
  val searchConverterService = mock[SearchConverterService]
  val searchService = mock[SearchService]
  val searchIndexService = mock[SearchIndexService]
  val converterService = org.mockito.Mockito.spy(new ConverterService)
  val clock = mock[SystemClock]
  val ndlaClient = mock[NdlaClient]
  val imageApiClient = mock[ImageApiClient]
  val articleApiClient = mock[ArticleApiClient]
  val keywordsService = mock[KeywordsService]
  val migrationApiClient = mock[MigrationApiClient]
  val importService = mock[ImportService]
  val languageValidator = mock[LanguageValidator]
  val learningpathController = mock[LearningpathController]
  val learningpathControllerV2 = mock[LearningpathControllerV2]
  val healthController = mock[HealthController]
  val learningStepValidator = mock[LearningStepValidator]
  val learningPathValidator = mock[LearningPathValidator]
  val titleValidator = mock[TitleValidator]
  val jestClient = mock[NdlaJestClient]

  def resetMocks() = {
    Mockito.reset(
      datasource, learningPathRepository, readService,
      updateService, searchService, searchIndexService, converterService, searchConverterService,
      languageValidator, titleValidator, jestClient, articleApiClient
    )
  }
}
