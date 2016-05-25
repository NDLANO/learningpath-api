package no.ndla.learningpathapi.batch

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.batch.integration.{CMDataComponent, KeywordsServiceComponent, PackageDataComponent}
import no.ndla.learningpathapi.batch.service.ImportServiceComponent
import no.ndla.learningpathapi.integration.DatasourceComponent
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}


object BatchComponentRegistry
  extends ImportServiceComponent
  with LearningPathRepositoryComponent
  with CMDataComponent
  with PackageDataComponent
  with DatasourceComponent
  with KeywordsServiceComponent {

  val CMPassword = scala.util.Properties.envOrNone("CM_PASSWORD")
  val CMUser = scala.util.Properties.envOrNone("CM_USER")
  val CMHost = scala.util.Properties.envOrNone("CM_HOST")
  val CMPort = scala.util.Properties.envOrNone("CM_PORT")
  val CMDatabase = scala.util.Properties.envOrNone("CM_DATABASE")

  val PackagePassword = scala.util.Properties.envOrNone("PACKAGE_PASSWORD")
  val PackageUser = scala.util.Properties.envOrNone("PACKAGE_USER")
  val PackageHost = scala.util.Properties.envOrNone("PACKAGE_HOST")
  val PackagePort = scala.util.Properties.envOrNone("PACKAGE_PORT")
  val PackageDatabase = scala.util.Properties.envOrNone("PACKAGE_DATABASE")

  lazy val cmData = new CMData(CMHost, CMPort, CMDatabase, CMUser, CMPassword)
  lazy val packageData = new PackageData(PackageHost, PackagePort, PackageDatabase, PackageUser, PackagePassword)

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

  val learningPathRepository = new LearningPathRepository
  val importService = new ImportService
  val keywordsService = new KeywordsService
}
