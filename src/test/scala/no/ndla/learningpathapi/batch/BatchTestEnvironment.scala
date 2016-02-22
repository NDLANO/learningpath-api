package no.ndla.learningpathapi.batch

import javax.sql.DataSource

import no.ndla.learningpathapi.batch.integration.{KeywordsServiceComponent, PackageDataComponent, CMDataComponent}
import no.ndla.learningpathapi.batch.service.ImportServiceComponent
import no.ndla.learningpathapi.integration.DatasourceComponent
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import org.scalatest.mock.MockitoSugar


trait BatchTestEnvironment
  extends ImportServiceComponent
  with LearningPathRepositoryComponent
  with CMDataComponent
  with PackageDataComponent
  with DatasourceComponent
  with KeywordsServiceComponent
  with MockitoSugar {

  val datasource = mock[DataSource]
  val packageData = mock[PackageData]
  val cmData = mock[CMData]
  val learningPathRepository = mock[LearningPathRepository]
  val importService = mock[ImportService]
  val keywordsService = mock[KeywordsService]
}
