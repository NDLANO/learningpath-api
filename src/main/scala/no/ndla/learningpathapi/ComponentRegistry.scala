package no.ndla.learningpathapi

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.learningpathapi.integration.{AuthClientComponent, DatasourceComponent, ElasticClientComponent, OEmbedClientComponent}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service.{Clock, _}
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexBuilderServiceComponent, SearchIndexServiceComponent, SearchServiceComponent}
import org.elasticsearch.common.settings.Settings
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}


object ComponentRegistry
  extends LearningPathRepositoryComponent
  with ReadServiceComponent
  with UpdateServiceComponent
  with SearchConverterServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with SearchIndexBuilderServiceComponent
  with AuthClientComponent
  with OEmbedClientComponent
  with ConverterServiceComponent
  with ElasticClientComponent
  with DatasourceComponent
  with Clock
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

  ConnectionPool.singleton(new DataSourceConnectionPool(datasource))


  lazy val elasticClient = ElasticClient.transport(
    Settings.settingsBuilder().put("cluster.name", LearningpathApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://${LearningpathApiProperties.SearchHost}:${LearningpathApiProperties.SearchPort}"))

  lazy val searchIndexBuilderService = new SearchIndexBuilderService

  lazy val learningPathRepository = new LearningPathRepository
  lazy val readService = new ReadService
  lazy val updateService = new UpdateService
  lazy val searchConverterService = new SearchConverterService
  lazy val searchService = new SearchService
  lazy val searchIndexService = new SearchIndexService
  lazy val authClient = new AuthClient
  lazy val oEmbedClient = new OEmbedClient
  lazy val converterService = new ConverterService
  lazy val clock = new SystemClock
}
