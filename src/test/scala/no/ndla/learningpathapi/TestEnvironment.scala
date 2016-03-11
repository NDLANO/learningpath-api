package no.ndla.learningpathapi

import javax.sql.DataSource

import com.sksamuel.elastic4s.ElasticClient
import no.ndla.learningpathapi.integration.{OEmbedClientComponent, AuthClientComponent, ElasticClientComponent, DatasourceComponent}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchServiceComponent, SearchIndexServiceComponent, SearchIndexBuilderServiceComponent}
import no.ndla.learningpathapi.validation._
import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar

trait TestEnvironment
  extends LearningPathRepositoryComponent
  with PublicServiceComponent
  with PrivateServiceComponent
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
  with CoverPhotoValidatorComponent
  with DescriptionValidatorComponent
  with DurationValidatorComponent
  with EmbedContentValidatorComponent
  with LanguageValidatorComponent
  with LicenseValidatorComponent
  with StatusValidatorComponent
  with StepTypeValidatorComponent
  with TagsValidatorComponent
  with TextValidatorComponent
  with TitleValidatorComponent
  with NewLearningPathValidatorComponent
  with NewLearningStepValidatorComponent
  with MockitoSugar {

  val datasource = mock[DataSource]
  val elasticClient = mock[ElasticClient]
  val searchIndexBuilderService = mock[SearchIndexBuilderService]

  val learningPathRepository = mock[LearningPathRepository]
  val publicService = mock[PublicService]
  val privateService = mock[PrivateService]
  val updateService = mock[UpdateService]
  val searchConverterService = mock[SearchConverterService]
  val searchService = mock[SearchService]
  val searchIndexService = mock[SearchIndexService]
  val authClient = mock[AuthClient]
  val oEmbedClient = mock[OEmbedClient]
  val converterService = org.mockito.Mockito.spy(new ConverterService)

  val coverPhotoValidator = mock[CoverPhotoValidator]
  val durationValidator = mock[DurationValidator]
  val embedContentValidator = mock[EmbedContentValidator]
  val languageValidator = mock[LanguageValidator]
  val licenseValidator = mock[LicenseValidator]
  val statusValidator = mock[StatusValidator]
  val stepTypeValidator = mock[StepTypeValidator]
  val tagsValidator = mock[TagsValidator]
  val noHtmlTextValidator = mock[TextValidator]
  val basicHtmlTextValidator = mock[TextValidator]
  val basicHtmlDescriptionValidator = mock[DescriptionValidator]
  val noHtmlDescriptionValidator = mock[DescriptionValidator]
  val titleValidator = mock[TitleValidator]
  val newLearningPathValidator = mock[NewLearningPathValidator]
  val newLearningStepValidator = mock[NewLearningStepValidator]



  def resetMocks() = {
    Mockito.reset(
      datasource, elasticClient, searchIndexBuilderService, learningPathRepository, publicService, privateService, updateService, searchService, searchIndexService, authClient, oEmbedClient,
      converterService, coverPhotoValidator, durationValidator, embedContentValidator, languageValidator, licenseValidator, statusValidator, stepTypeValidator, tagsValidator,
      noHtmlTextValidator, basicHtmlTextValidator, basicHtmlDescriptionValidator, noHtmlDescriptionValidator, titleValidator, newLearningPathValidator, newLearningStepValidator)
  }
}
