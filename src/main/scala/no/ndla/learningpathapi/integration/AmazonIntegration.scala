package no.ndla.learningpathapi.integration

import com.sksamuel.elastic4s.{ElasticsearchClientUri, ElasticClient}
import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.business.{UserData, LearningPathSearch, LearningPathIndex, LearningpathData}
import no.ndla.learningpathapi.service.{ModelConverters, UpdateService, PrivateService, PublicService}
import org.elasticsearch.common.settings.ImmutableSettings
import org.postgresql.ds.PGPoolingDataSource


object AmazonIntegration {

  private val datasource = new PGPoolingDataSource()
  datasource.setUser(LearningpathApiProperties.MetaUserName)
  datasource.setPassword(LearningpathApiProperties.MetaPassword)
  datasource.setDatabaseName(LearningpathApiProperties.MetaResource)
  datasource.setServerName(LearningpathApiProperties.MetaServer)
  datasource.setPortNumber(LearningpathApiProperties.MetaPort)
  datasource.setInitialConnections(LearningpathApiProperties.MetaInitialConnections)
  datasource.setMaxConnections(LearningpathApiProperties.MetaMaxConnections)
  datasource.setCurrentSchema(LearningpathApiProperties.MetaSchema)

  def getPublicService(): PublicService = {
    new PublicService(getLearningpathData(), getModelConverter())
  }

  def getPrivateService(): PrivateService = {
    new PrivateService(getLearningpathData(), getModelConverter())
  }

  def getUpdateService(): UpdateService = {
    new UpdateService(getLearningpathData(), getLearningPathIndex(), getModelConverter())
  }

  def getLearningpathData(): LearningpathData = {
    new PostgresData(datasource)
  }

  def getLearningPathIndex(): LearningPathIndex = {
    new ElasticLearningPathIndex(createElasticClient, getModelConverter())
  }

  def getLearningPathSearch(): LearningPathSearch = {
    new ElasticLearningPathSearch(createElasticClient, getModelConverter())
  }

  def getModelConverter(): ModelConverters = {
    new ModelConverters(getUserData())
  }

  def getUserData(): UserData = {
    new AuthUserDataClient()
  }

  private def createElasticClient: ElasticClient = {
    ElasticClient.remote(
      ImmutableSettings.settingsBuilder().put("cluster.name", LearningpathApiProperties.SearchClusterName).build(),
      ElasticsearchClientUri(s"elasticsearch://${LearningpathApiProperties.SearchHost}:${LearningpathApiProperties.SearchPort}"))
  }
}
