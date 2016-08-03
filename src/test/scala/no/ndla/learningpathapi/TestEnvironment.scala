package no.ndla.learningpathapi

import javax.sql.DataSource

import com.sksamuel.elastic4s.ElasticClient
import no.ndla.learningpathapi.controller.LearningpathController
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexBuilderServiceComponent, SearchIndexServiceComponent, SearchServiceComponent}
import no.ndla.learningpathapi.validation._
import no.ndla.network.NdlaClient
import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar

trait TestEnvironment
  extends LearningpathController
  with LearningPathRepositoryComponent
  with ReadServiceComponent
  with UpdateServiceComponent
  with SearchConverterServiceComponent
  with SearchServiceComponent
  with SearchIndexServiceComponent
  with SearchIndexBuilderServiceComponent
  with NdlaClient
  with AuthClientComponent
  with OEmbedClientComponent
  with ImageApiClientComponent
  with ConverterServiceComponent
  with ElasticClientComponent
  with DatasourceComponent
  with MockitoSugar
  with KeywordsServiceComponent
  with ImportServiceComponent
  with MigrationApiClient
  with Clock
  with LanguageValidator
  with LearningPathValidator
  with LearningStepValidator
  with MappingApiClient
  with TitleValidator {

  val datasource = mock[DataSource]
  val elasticClient = mock[ElasticClient]
  val searchIndexBuilderService = mock[SearchIndexBuilderService]

  val learningPathRepository = mock[LearningPathRepository]
  val readService = mock[ReadService]
  val updateService = mock[UpdateService]
  val searchConverterService = mock[SearchConverterService]
  val searchService = mock[SearchService]
  val searchIndexService = mock[SearchIndexService]
  val authClient = mock[AuthClient]
  val oEmbedClient = mock[OEmbedClient]
  val converterService = org.mockito.Mockito.spy(new ConverterService)
  val clock = mock[SystemClock]
  val ndlaClient = mock[NdlaClient]
  val imageApiClient = mock[ImageApiClient]
  val keywordsService = mock[KeywordsService]
  val migrationApiClient = mock[MigrationApiClient]
  val importService = mock[ImportService]
  val languageValidator = mock[LanguageValidator]
  val learningpathController = mock[LearningpathController]
  val mappingApiClient = mock[MappingApiClient]
  val learningStepValidator = mock[LearningStepValidator]
  val learningPathValidator = mock[LearningPathValidator]
  val titleValidator = mock[TitleValidator]
  def resetMocks() = {
    Mockito.reset(
      datasource, elasticClient, searchIndexBuilderService, learningPathRepository, readService, updateService, searchService, searchIndexService, authClient, converterService, searchConverterService, oEmbedClient, languageValidator, titleValidator, mappingApiClient)
  }
}
