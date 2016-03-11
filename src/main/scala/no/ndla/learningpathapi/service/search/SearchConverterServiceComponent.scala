package no.ndla.learningpathapi.service.search

import no.ndla.learningpathapi.integration.AuthClientComponent
import no.ndla.learningpathapi.model.api.{Author, LearningPathSummary}
import no.ndla.learningpathapi.model.domain.{LearningStep, LearningPath}
import no.ndla.learningpathapi.model.search.{SearchableLearningStep, SearchableLearningPath}
import no.ndla.learningpathapi.service.ConverterServiceComponent
import no.ndla.network.ApplicationUrl

trait SearchConverterServiceComponent {
  this: AuthClientComponent with ConverterServiceComponent =>
  val searchConverterService: SearchConverterService

  class SearchConverterService {

    def asApiLearningPathSummary(searchableLearningPath: SearchableLearningPath): LearningPathSummary = {
      LearningPathSummary(
        searchableLearningPath.id,
        searchableLearningPath.title.map(converterService.asApiTitle),
        searchableLearningPath.description.map(converterService.asApiDescription),
        createUrlToLearningPath(searchableLearningPath.id),
        searchableLearningPath.coverPhotoUrl,
        searchableLearningPath.duration,
        searchableLearningPath.status,
        searchableLearningPath.lastUpdated,
        asAuthor(searchableLearningPath.author)
      )
    }


    def asSearchableLearningpath(learningPath: LearningPath): SearchableLearningPath = {
      SearchableLearningPath(
        learningPath.id.get,
        learningPath.title,
        learningPath.description,
        learningPath.coverPhotoUrl,
        learningPath.duration,
        learningPath.status.toString,
        learningPath.verificationStatus.toString,
        learningPath.lastUpdated,
        learningPath.tags,
        getAuthor(learningPath.owner),
        learningPath.learningsteps.map(asSearchableLearningStep).toList)
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      SearchableLearningStep(learningStep.title, learningStep.description)
    }

    def getAuthor(owner: String): String = {
      converterService.asAuthor(authClient.getUserName(owner)).name
    }

    def asAuthor(author: String): Author = {
      Author("Forfatter", author)
    }

    def createUrlToLearningPath(id: Long): String = {
      s"${ApplicationUrl.get()}$id"
    }
  }
}
