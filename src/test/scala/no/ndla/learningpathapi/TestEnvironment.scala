/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import javax.sql.DataSource

import no.ndla.learningpathapi.controller.{HealthController, LearningpathControllerV2}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexServiceComponent, SearchServiceComponent}
import no.ndla.learningpathapi.validation._
import no.ndla.network.NdlaClient
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
  extends LearningpathControllerV2
  with LearningPathRepositoryComponent
  with ReadServiceComponent
  with UpdateServiceComponent
  with SearchConverterServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with TaxonomyApiClient
  with NdlaClient
  with ImageApiClientComponent
  with ArticleImportClient
  with MigrationApiClient
  with ConverterServiceComponent
  with Elastic4sClient
  with DatasourceComponent
  with MockitoSugar
  with KeywordsServiceComponent
  with ImportService
  with Clock
  with HealthController
  with LanguageValidator
  with LearningPathValidator
  with LearningStepValidator
  with TitleValidator {

  val dataSource = mock[DataSource]

  val learningPathRepository = mock[LearningPathRepository]
  val readService = mock[ReadService]
  val updateService = mock[UpdateService]
  val searchConverterService = mock[SearchConverterService]
  val searchService = mock[SearchService]
  val searchIndexService = mock[SearchIndexService]
  val converterService = org.mockito.Mockito.spy(new ConverterService)
  val clock = mock[SystemClock]
  val taxononyApiClient = mock[TaxonomyApiClient]
  val ndlaClient = mock[NdlaClient]
  val imageApiClient = mock[ImageApiClient]
  val articleImportClient = mock[ArticleImportClient]
  val keywordsService = mock[KeywordsService]
  val migrationApiClient = mock[MigrationApiClient]
  val importService = mock[ImportService]
  val languageValidator = mock[LanguageValidator]
  val learningpathControllerV2 = mock[LearningpathControllerV2]
  val healthController = mock[HealthController]
  val learningStepValidator = mock[LearningStepValidator]
  val learningPathValidator = mock[LearningPathValidator]
  val titleValidator = mock[TitleValidator]
  val e4sClient = mock[NdlaE4sClient]

  def resetMocks() = {
    Mockito.reset(
      dataSource, learningPathRepository, readService,
      updateService, searchService, searchIndexService, converterService, searchConverterService,
      languageValidator, titleValidator, e4sClient, articleImportClient
    )
  }
}
