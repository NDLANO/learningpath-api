/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.learningpathapi.controller.{
  ConfigController,
  HealthController,
  InternController,
  LearningpathControllerV2
}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.repository.{ConfigRepository, LearningPathRepositoryComponent}
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation._
import no.ndla.network.NdlaClient
import org.mockito.Mockito
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends LearningpathControllerV2
    with ConfigController
    with LearningPathRepositoryComponent
    with ConfigRepository
    with ReadService
    with UpdateService
    with SearchConverterServiceComponent
    with SearchService
    with SearchIndexService
    with SearchApiClient
    with TaxonomyApiClient
    with NdlaClient
    with ImageApiClientComponent
    with ArticleImportClient
    with MigrationApiClient
    with ConverterService
    with Elastic4sClient
    with DataSource
    with MockitoSugar
    with KeywordsServiceComponent
    with ImportService
    with Clock
    with HealthController
    with LanguageValidator
    with LearningPathValidator
    with LearningStepValidator
    with TitleValidator
    with InternController {

  val dataSource: HikariDataSource = mock[HikariDataSource]

  val learningPathRepository: LearningPathRepository = mock[LearningPathRepository]
  val learningPathRepositoryComponent: LearningPathRepositoryComponent = mock[LearningPathRepositoryComponent]
  val configRepository: ConfigRepository = mock[ConfigRepository]
  val readService: ReadService = mock[ReadService]
  val updateService: UpdateService = mock[UpdateService]
  val searchConverterService: SearchConverterService = mock[SearchConverterService]
  val searchService: SearchService = mock[SearchService]
  val searchIndexService: SearchIndexService = mock[SearchIndexService]
  val converterService: ConverterService = org.mockito.Mockito.spy(new ConverterService)
  val clock: SystemClock = mock[SystemClock]
  val taxononyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]
  val ndlaClient: NdlaClient = mock[NdlaClient]
  val imageApiClient: ImageApiClient = mock[ImageApiClient]
  val articleImportClient: ArticleImportClient = mock[ArticleImportClient]
  val keywordsService: KeywordsService = mock[KeywordsService]
  val migrationApiClient: MigrationApiClient = mock[MigrationApiClient]
  val importService: ImportService = mock[ImportService]
  val languageValidator: LanguageValidator = mock[LanguageValidator]
  val learningpathControllerV2: LearningpathControllerV2 = mock[LearningpathControllerV2]
  val configController: ConfigController = mock[ConfigController]
  val healthController: HealthController = mock[HealthController]
  val internController: InternController = mock[InternController]
  val learningStepValidator: LearningStepValidator = mock[LearningStepValidator]
  val learningPathValidator: LearningPathValidator = mock[LearningPathValidator]
  val titleValidator: TitleValidator = mock[TitleValidator]
  var e4sClient: NdlaE4sClient = mock[NdlaE4sClient]
  val searchApiClient: SearchApiClient = mock[SearchApiClient]

  def resetMocks(): Unit = {
    Mockito.reset(
      dataSource,
      learningPathRepository,
      configRepository,
      readService,
      updateService,
      searchService,
      searchIndexService,
      converterService,
      searchConverterService,
      languageValidator,
      titleValidator,
      e4sClient,
      articleImportClient
    )
  }
}
