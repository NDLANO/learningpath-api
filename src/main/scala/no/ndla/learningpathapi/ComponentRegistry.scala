package no.ndla.learningpathapi

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.learningpathapi.integration.{ElasticClientComponent, DatasourceComponent}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchServiceComponent, SearchIndexServiceComponent, SearchIndexBuilderServiceComponent}
import org.elasticsearch.common.settings.ImmutableSettings
import org.postgresql.ds.PGPoolingDataSource


object ComponentRegistry
  extends LearningPathRepositoryComponent
  with PublicServiceComponent
  with PrivateServiceComponent
  with UpdateServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with SearchIndexBuilderServiceComponent
  with ElasticClientComponent
  with DatasourceComponent
{
  lazy val datasource = new PGPoolingDataSource()
  datasource.setUser(LearningpathApiProperties.MetaUserName)
  datasource.setPassword(LearningpathApiProperties.MetaPassword)
  datasource.setDatabaseName(LearningpathApiProperties.MetaResource)
  datasource.setServerName(LearningpathApiProperties.MetaServer)
  datasource.setPortNumber(LearningpathApiProperties.MetaPort)
  datasource.setInitialConnections(LearningpathApiProperties.MetaInitialConnections)
  datasource.setMaxConnections(LearningpathApiProperties.MetaMaxConnections)
  datasource.setCurrentSchema(LearningpathApiProperties.MetaSchema)

  lazy val elasticClient = ElasticClient.remote(
    ImmutableSettings.settingsBuilder().put("cluster.name", LearningpathApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://${LearningpathApiProperties.SearchHost}:${LearningpathApiProperties.SearchPort}"))

  lazy val searchIndexBuilderService = new SearchIndexBuilderService

  lazy val learningPathRepository = new LearningPathRepository
  lazy val publicService = new PublicService
  lazy val privateService = new PrivateService
  lazy val updateService = new UpdateService
  lazy val searchService = new SearchService
  lazy val searchIndexService = new SearchIndexService


}
