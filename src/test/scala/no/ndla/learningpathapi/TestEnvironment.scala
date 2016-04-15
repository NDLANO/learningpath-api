package no.ndla.learningpathapi

import javax.sql.DataSource

import com.sksamuel.elastic4s.ElasticClient
import no.ndla.learningpathapi.integration.{AuthClientComponent, DatasourceComponent, ElasticClientComponent}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchIndexBuilderServiceComponent, SearchIndexServiceComponent, SearchServiceComponent}
import no.ndla.learningpathapi.validation._
import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar
import scalikejdbc.DBSession

trait TestEnvironment
  extends LearningPathRepositoryComponent
  with ReadServiceComponent
  with UpdateServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with SearchIndexBuilderServiceComponent
  with AuthClientComponent
  with ConverterServiceComponent
  with ElasticClientComponent
  with DatasourceComponent
  with MockitoSugar {

  val datasource = mock[DataSource]
  val elasticClient = mock[ElasticClient]
  val searchIndexBuilderService = mock[SearchIndexBuilderService]

  val learningPathRepository = mock[LearningPathRepository]
  val readService = mock[ReadService]
  val updateService = mock[UpdateService]
  val searchService = mock[SearchService]
  val searchIndexService = mock[SearchIndexService]
  val authClient = mock[AuthClient]
  val converterService = org.mockito.Mockito.spy(new ConverterService)

  def resetMocks() = {
    Mockito.reset(
      datasource, elasticClient, searchIndexBuilderService, learningPathRepository, readService, updateService, searchService, searchIndexService, authClient, converterService)
  }
}
