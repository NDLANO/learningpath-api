package no.ndla.learningpathapi

import javax.sql.DataSource

import com.sksamuel.elastic4s.ElasticClient
import no.ndla.learningpathapi.integration.{ElasticClientComponent, DatasourceComponent}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchServiceComponent, SearchIndexServiceComponent, SearchIndexBuilderServiceComponent}
import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar

trait TestEnvironment
  extends LearningPathRepositoryComponent
  with PublicServiceComponent
  with PrivateServiceComponent
  with UpdateServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with SearchIndexBuilderServiceComponent
  with ElasticClientComponent
  with DatasourceComponent
  with MockitoSugar {

  val datasource = mock[DataSource]
  val elasticClient = mock[ElasticClient]
  val searchIndexBuilderService = mock[SearchIndexBuilderService]

  val learningPathRepository = mock[LearningPathRepository]
  val publicService = mock[PublicService]
  val privateService = mock[PrivateService]
  val updateService = mock[UpdateService]
  val searchService = mock[SearchService]
  val searchIndexService = mock[SearchIndexService]

  def resetMocks() = {
    Mockito.reset(datasource, elasticClient, searchIndexBuilderService, learningPathRepository, publicService, privateService, updateService, searchService, searchIndexService)
  }
}
