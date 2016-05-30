package no.ndla.learningpathapi.service.search

import no.ndla.learningpathapi.integration.AuthClientComponent
import no.ndla.learningpathapi.model.api.{Author, LearningPathSummary}
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.search._
import no.ndla.learningpathapi.service.ConverterServiceComponent
import no.ndla.mapping.ISO639Mapping
import no.ndla.network.ApplicationUrl


trait SearchConverterServiceComponent {
  this: AuthClientComponent with ConverterServiceComponent =>
  val searchConverterService: SearchConverterService

  class SearchConverterService {

    def asApiTitle(titles: SearchableTitles): List[api.Title] = {
      List((titles.zh, Some(ISO639Mapping.CHINESE)),
        (titles.en, Some(ISO639Mapping.ENGLISH)),
        (titles.fr, Some(ISO639Mapping.FRENCH)),
        (titles.de, Some(ISO639Mapping.GERMAN)),
        (titles.nb, Some(ISO639Mapping.NORWEGIAN_BOKMAL)),
        (titles.nn, Some(ISO639Mapping.NORWEGIAN_NYNORSK)),
        (titles.se, Some(ISO639Mapping.SAMI)),
        (titles.es, Some(ISO639Mapping.SPANISH)),
        (titles.unknown, None)
      ).filter(_._1.isDefined).map(tuple => api.Title(tuple._1.get, tuple._2))
    }

    def asApiDescription(descriptions: SearchableDescriptions): List[api.Description] = {
      List((descriptions.zh, Some(ISO639Mapping.CHINESE)),
        (descriptions.en, Some(ISO639Mapping.ENGLISH)),
        (descriptions.fr, Some(ISO639Mapping.FRENCH)),
        (descriptions.de, Some(ISO639Mapping.GERMAN)),
        (descriptions.nb, Some(ISO639Mapping.NORWEGIAN_BOKMAL)),
        (descriptions.nn, Some(ISO639Mapping.NORWEGIAN_NYNORSK)),
        (descriptions.se, Some(ISO639Mapping.SAMI)),
        (descriptions.es, Some(ISO639Mapping.SPANISH)),
        (descriptions.unknown, None)
      ).filter(_._1.isDefined).map(tuple => api.Description(tuple._1.get, tuple._2))
    }

    def asApiLearningPathSummary(searchableLearningPath: SearchableLearningPath): LearningPathSummary = {
      LearningPathSummary(
        searchableLearningPath.id,
        asApiTitle(searchableLearningPath.titles),
        asApiDescription(searchableLearningPath.descriptions),
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
        asSearchableTitles(learningPath.title),
        asSearchableDescriptions(learningPath.description),
        learningPath.coverPhotoUrl,
        learningPath.duration,
        learningPath.status.toString,
        learningPath.verificationStatus.toString,
        learningPath.lastUpdated,
        asSearchableTags(learningPath.tags),
        getAuthor(learningPath.owner),
        learningPath.learningsteps.map(asSearchableLearningStep).toList)
    }

    def asSearchableTags(tags: List[LearningPathTag]): SearchableTags = {
      SearchableTags(
        nb = tags.filter(_.language.contains("nb")).map(_.tag),
        nn = tags.filter(_.language.contains("nn")).map(_.tag),
        en = tags.filter(_.language.contains("en")).map(_.tag),
        fr = tags.filter(_.language.contains("fr")).map(_.tag),
        de = tags.filter(_.language.contains("de")).map(_.tag),
        es = tags.filter(_.language.contains("es")).map(_.tag),
        se = tags.filter(_.language.contains("se")).map(_.tag),
        zh = tags.filter(_.language.contains("zh")).map(_.tag),
        unknown = tags.filter(_.language.isEmpty).map(_.tag)
      )
    }

    def asSearchableDescriptions(description: List[Description]): SearchableDescriptions = {
      SearchableDescriptions(
        nb = description.find(_.language.contains(ISO639Mapping.NORWEGIAN_BOKMAL)).map(_.description),
        nn = description.find(_.language.contains(ISO639Mapping.NORWEGIAN_NYNORSK)).map(_.description),
        en = description.find(_.language.contains(ISO639Mapping.ENGLISH)).map(_.description),
        fr = description.find(_.language.contains(ISO639Mapping.FRENCH)).map(_.description),
        de = description.find(_.language.contains(ISO639Mapping.GERMAN)).map(_.description),
        es = description.find(_.language.contains(ISO639Mapping.SPANISH)).map(_.description),
        se = description.find(_.language.contains(ISO639Mapping.SAMI)).map(_.description),
        zh = description.find(_.language.contains(ISO639Mapping.CHINESE)).map(_.description),
        unknown = description.find(_.language.isEmpty).map(_.description)
      )
    }

    def asSearchableTitles(title: List[Title]): SearchableTitles = {
      SearchableTitles(
        nb = title.find(_.language.contains(ISO639Mapping.NORWEGIAN_BOKMAL)).map(_.title),
        nn = title.find(_.language.contains(ISO639Mapping.NORWEGIAN_NYNORSK)).map(_.title),
        en = title.find(_.language.contains(ISO639Mapping.ENGLISH)).map(_.title),
        fr = title.find(_.language.contains(ISO639Mapping.FRENCH)).map(_.title),
        de = title.find(_.language.contains(ISO639Mapping.GERMAN)).map(_.title),
        es = title.find(_.language.contains(ISO639Mapping.SPANISH)).map(_.title),
        se = title.find(_.language.contains(ISO639Mapping.SAMI)).map(_.title),
        zh = title.find(_.language.contains(ISO639Mapping.CHINESE)).map(_.title),
        unknown = title.find(_.language.isEmpty).map(_.title)
      )
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      SearchableLearningStep(asSearchableTitles(learningStep.title), asSearchableDescriptions(learningStep.description))
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
