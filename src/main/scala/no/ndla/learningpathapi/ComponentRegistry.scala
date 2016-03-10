package no.ndla.learningpathapi

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.learningpathapi.integration.{AuthClientComponent, DatasourceComponent, ElasticClientComponent, OEmbedClientComponent}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchIndexBuilderServiceComponent, SearchIndexServiceComponent, SearchServiceComponent}
import no.ndla.learningpathapi.validation._
import org.elasticsearch.common.settings.ImmutableSettings
import org.postgresql.ds.PGPoolingDataSource


object ComponentRegistry
  extends LearningPathRepositoryComponent
  with PublicServiceComponent
  with PrivateServiceComponent
  with UpdateServiceComponent
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

  lazy val elasticClient = ElasticClient.remote(
    ImmutableSettings.settingsBuilder().put("cluster.name", LearningpathApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://${LearningpathApiProperties.SearchHost}:${LearningpathApiProperties.SearchPort}"))

  lazy val searchIndexBuilderService = new SearchIndexBuilderService

  lazy val learningPathRepository = new LearningPathRepository
  lazy val publicService = new PublicService
  lazy val privateService = new PrivateService
  lazy val updateService = new UpdateService
  lazy val searchService = new SearchService
  lazy val searchIndexService = new SearchIndexService
  lazy val authClient = new AuthClient
  lazy val oEmbedClient = new OEmbedClient
  lazy val converterService = new ConverterService

  lazy val coverPhotoValidator = new CoverPhotoValidator
  lazy val durationValidator = new DurationValidator
  lazy val embedContentValidator = new EmbedContentValidator
  lazy val languageValidator = new LanguageValidator
  lazy val licenseValidator = new LicenseValidator
  lazy val statusValidator = new StatusValidator
  lazy val stepTypeValidator = new StepTypeValidator
  lazy val tagsValidator = new TagsValidator
  lazy val noHtmlTextValidator = new TextValidator(allowHtml = false)
  lazy val basicHtmlTextValidator = new TextValidator(allowHtml = true)
  lazy val basicHtmlDescriptionValidator = new DescriptionValidator(allowHtml = true)
  lazy val noHtmlDescriptionValidator = new DescriptionValidator(allowHtml = false)
  lazy val titleValidator = new TitleValidator
  lazy val newLearningPathValidator = new NewLearningPathValidator
  lazy val newLearningStepValidator = new NewLearningStepValidator
}
