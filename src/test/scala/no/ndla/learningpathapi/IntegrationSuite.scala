package no.ndla.learningpathapi

import java.util.Properties
import javax.sql.DataSource

import org.postgresql.ds.PGPoolingDataSource
import org.scalatest.Tag

import scala.collection.JavaConversions.propertiesAsScalaMap

abstract class IntegrationSuite extends UnitSuite {
  val TestProperties = "integration-test.properties"
  println(s"Reading properties from $TestProperties")

  val integrationTestProperties = new Properties()
  integrationTestProperties.load(getClass.getResourceAsStream(s"/$TestProperties"))

  val learningPathApiProperties = propertiesAsScalaMap(integrationTestProperties).map(entry => (entry._1, Option(entry._2))).toMap
  LearningpathApiProperties.setProperties(learningPathApiProperties)

  def getDataSource(): DataSource = {
    val datasource = new PGPoolingDataSource()
    datasource.setUser(LearningpathApiProperties.MetaUserName)
    datasource.setPassword(LearningpathApiProperties.MetaPassword)
    datasource.setDatabaseName(LearningpathApiProperties.MetaResource)
    datasource.setServerName(LearningpathApiProperties.MetaServer)
    datasource.setPortNumber(LearningpathApiProperties.MetaPort)
    datasource.setInitialConnections(LearningpathApiProperties.MetaInitialConnections)
    datasource.setMaxConnections(LearningpathApiProperties.MetaMaxConnections)
    datasource.setCurrentSchema(LearningpathApiProperties.MetaSchema)
    datasource
  }
}
