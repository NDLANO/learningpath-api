/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.learningpathapi
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
import no.ndla.learningpathapi.validation.{
  LanguageValidator,
  LearningPathValidator,
  LearningStepValidator,
  TitleValidator
}
import no.ndla.network.NdlaClient
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
    extends LearningpathControllerV2
    with InternController
    with HealthController
    with ConfigController
    with LearningPathRepositoryComponent
    with ConfigRepository
    with ReadService
    with UpdateService
    with SearchConverterServiceComponent
    with SearchService
    with SearchIndexService
    with TaxonomyApiClient
    with NdlaClient
    with ImageApiClientComponent
    with ArticleImportClient
    with MigrationApiClient
    with ConverterService
    with Elastic4sClient
    with DataSource
    with ImportService
    with KeywordsServiceComponent
    with Clock
    with LanguageValidator
    with LearningPathValidator
    with LearningStepValidator
    with TitleValidator
    with SearchApiClient {

  def connectToDatabase(): Unit = ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
  lazy val dataSource: HikariDataSource = DataSource.getHikariDataSource
  connectToDatabase()

  implicit val swagger: LearningpathSwagger = new LearningpathSwagger

  lazy val learningPathRepository = new LearningPathRepository
  lazy val configRepository = new ConfigRepository
  lazy val readService = new ReadService
  lazy val updateService = new UpdateService
  lazy val searchConverterService = new SearchConverterService
  lazy val searchService = new SearchService
  lazy val searchIndexService = new SearchIndexService
  lazy val converterService = new ConverterService
  lazy val clock = new SystemClock
  lazy val learningpathControllerV2 = new LearningpathControllerV2
  lazy val internController = new InternController
  lazy val configController = new ConfigController
  lazy val resourcesApp = new ResourcesApp
  lazy val taxononyApiClient = new TaxonomyApiClient
  lazy val ndlaClient = new NdlaClient
  lazy val imageApiClient = new ImageApiClient
  lazy val articleImportClient = new ArticleImportClient
  lazy val importService = new ImportService
  lazy val keywordsService = new KeywordsService
  lazy val migrationApiClient = new MigrationApiClient
  lazy val healthController = new HealthController
  lazy val languageValidator = new LanguageValidator
  lazy val titleValidator = new TitleValidator
  lazy val learningPathValidator = new LearningPathValidator
  lazy val learningStepValidator = new LearningStepValidator
  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient()
  lazy val searchApiClient = new SearchApiClient
}
