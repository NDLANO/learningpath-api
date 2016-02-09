package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.business.{LearningPathSearch, LearningPathIndex, LearningpathData}
import no.ndla.learningpathapi.service.{UpdateService, PrivateService, PublicService}
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
    new PublicService(getLearningpathData())
  }

  def getPrivateService(): PrivateService = {
    new PrivateService(getLearningpathData())
  }

  def getUpdateService(): UpdateService = {
    new UpdateService(getLearningpathData(), getLearningPathIndex())
  }

  def getLearningpathData(): LearningpathData = {
    new PostgresData(datasource)
  }

  def getLearningPathIndex(): LearningPathIndex = {
    new ElasticLearningPathIndex(
      LearningpathApiProperties.SearchClusterName,
      LearningpathApiProperties.SearchHost,
      LearningpathApiProperties.SearchPort)
  }

  def getLearningPathSearch(): LearningPathSearch = {
    new ElasticLearningPathSearch(
      LearningpathApiProperties.SearchClusterName,
      LearningpathApiProperties.SearchHost,
      LearningpathApiProperties.SearchPort)
  }
}
