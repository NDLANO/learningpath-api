/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.learningpathapi.controller.{HealthController, InternController, LearningpathControllerV2}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexServiceComponent, SearchServiceComponent}
import no.ndla.learningpathapi.validation.{LanguageValidator, LearningPathValidator, LearningStepValidator, TitleValidator}
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}


object ComponentRegistry
  extends LearningpathControllerV2
  with InternController
  with HealthController
  with LearningPathRepositoryComponent
  with ReadServiceComponent
  with UpdateServiceComponent
  with SearchConverterServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with NdlaClient
  with ImageApiClientComponent
  with ArticleImportClient
  with MigrationApiClient
  with ConverterServiceComponent
  with Elastic4sClient
  with DatasourceComponent
  with ImportServiceComponent
  with KeywordsServiceComponent
  with Clock
  with LanguageValidator
  with LearningPathValidator
  with LearningStepValidator
  with TitleValidator {

  implicit val swagger = new LearningpathSwagger

  lazy val datasource = new PGPoolingDataSource()
  datasource.setUser(LearningpathApiProperties.MetaUserName)
  datasource.setPassword(LearningpathApiProperties.MetaPassword)
  datasource.setDatabaseName(LearningpathApiProperties.MetaResource)
  datasource.setServerName(LearningpathApiProperties.MetaServer)
  datasource.setPortNumber(LearningpathApiProperties.MetaPort)
  datasource.setInitialConnections(LearningpathApiProperties.MetaInitialConnections)
  datasource.setMaxConnections(LearningpathApiProperties.MetaMaxConnections)
  datasource.setCurrentSchema(LearningpathApiProperties.MetaSchema)

  ConnectionPool.singleton(new DataSourceConnectionPool(datasource))


  lazy val learningPathRepository = new LearningPathRepository
  lazy val readService = new ReadService
  lazy val updateService = new UpdateService
  lazy val searchConverterService = new SearchConverterService
  lazy val searchService = new SearchService
  lazy val searchIndexService = new SearchIndexService
  lazy val converterService = new ConverterService
  lazy val clock = new SystemClock
  lazy val learningpathControllerV2 = new LearningpathControllerV2
  lazy val internController = new InternController
  lazy val resourcesApp = new ResourcesApp
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
  lazy val e4sClient = Elastic4sClientFactory.getClient()
}
