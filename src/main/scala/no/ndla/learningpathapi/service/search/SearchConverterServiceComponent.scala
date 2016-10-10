/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import no.ndla.learningpathapi.integration.{AuthClientComponent, ImageApiClientComponent, MappingApiClient}
import no.ndla.learningpathapi.model.api.{Author, Introduction, LearningPathSummary}
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.search._
import no.ndla.learningpathapi.service.ConverterServiceComponent
import no.ndla.network.ApplicationUrl


trait SearchConverterServiceComponent {
  this: AuthClientComponent with ConverterServiceComponent with MappingApiClient with ImageApiClientComponent =>
  val searchConverterService: SearchConverterService

  class SearchConverterService {

    def asApiTitle(titles: SearchableTitles): List[api.Title] = {
      List((titles.zh, Some(Language.CHINESE)),
        (titles.en, Some(Language.ENGLISH)),
        (titles.fr, Some(Language.FRENCH)),
        (titles.de, Some(Language.GERMAN)),
        (titles.nb, Some(Language.NORWEGIAN_BOKMAL)),
        (titles.nn, Some(Language.NORWEGIAN_NYNORSK)),
        (titles.se, Some(Language.SAMI)),
        (titles.es, Some(Language.SPANISH)),
        (titles.unknown, None)
      ).filter(_._1.isDefined).map(tuple => api.Title(tuple._1.get, tuple._2))
    }

    def asApiDescription(descriptions: SearchableDescriptions): List[api.Description] = {
      List((descriptions.zh, Some(Language.CHINESE)),
        (descriptions.en, Some(Language.ENGLISH)),
        (descriptions.fr, Some(Language.FRENCH)),
        (descriptions.de, Some(Language.GERMAN)),
        (descriptions.nb, Some(Language.NORWEGIAN_BOKMAL)),
        (descriptions.nn, Some(Language.NORWEGIAN_NYNORSK)),
        (descriptions.se, Some(Language.SAMI)),
        (descriptions.es, Some(Language.SPANISH)),
        (descriptions.unknown, None)
      ).filter(_._1.isDefined).map(tuple => api.Description(tuple._1.get, tuple._2))
    }

    def asApiLearningPathTag(tags: SearchableTags): Seq[api.LearningPathTags] = {
      Seq(api.LearningPathTags(tags.zh, Some(Language.CHINESE)),
        api.LearningPathTags(tags.en, Some(Language.ENGLISH)),
        api.LearningPathTags(tags.fr, Some(Language.FRENCH)),
        api.LearningPathTags(tags.de, Some(Language.GERMAN)),
        api.LearningPathTags(tags.nb, Some(Language.NORWEGIAN_BOKMAL)),
        api.LearningPathTags(tags.nn, Some(Language.NORWEGIAN_NYNORSK)),
        api.LearningPathTags(tags.se, Some(Language.SAMI)),
        api.LearningPathTags(tags.es, Some(Language.SPANISH)),
        api.LearningPathTags(tags.unknown, Some(Language.UNKNOWN))).filterNot(_.tags.isEmpty)
    }

    def asApiIntroduction(learningStep: Option[SearchableLearningStep]): List[Introduction] = {
      learningStep.map(_.descriptions) match {
        case None => List()
        case Some(descriptions) => {
          List(
            (descriptions.zh, Some(Language.CHINESE)),
            (descriptions.en, Some(Language.ENGLISH)),
            (descriptions.fr, Some(Language.FRENCH)),
            (descriptions.de, Some(Language.GERMAN)),
            (descriptions.nb, Some(Language.NORWEGIAN_BOKMAL)),
            (descriptions.nn, Some(Language.NORWEGIAN_NYNORSK)),
            (descriptions.se, Some(Language.SAMI)),
            (descriptions.es, Some(Language.SPANISH)),
            (descriptions.unknown, Some(Language.UNKNOWN))
          ).filter(_._1.isDefined).map(tuple => Introduction(tuple._1.get, tuple._2))
        }
      }
    }

    def asApiLearningPathSummary(searchableLearningPath: SearchableLearningPath): LearningPathSummary = {
      LearningPathSummary(
        searchableLearningPath.id,
        asApiTitle(searchableLearningPath.titles),
        asApiDescription(searchableLearningPath.descriptions),
        asApiIntroduction(searchableLearningPath.learningsteps.find(_.stepType == StepType.INTRODUCTION.toString)),
        createUrlToLearningPath(searchableLearningPath.id),
        searchableLearningPath.coverPhotoUrl,
        searchableLearningPath.duration,
        searchableLearningPath.status,
        searchableLearningPath.lastUpdated,
        asApiLearningPathTag(searchableLearningPath.tags),
        asAuthor(searchableLearningPath.author),
        searchableLearningPath.copyright
      )
    }


    def asSearchableLearningpath(learningPath: LearningPath): SearchableLearningPath = {
      SearchableLearningPath(
        learningPath.id.get,
        asSearchableTitles(learningPath.title),
        asSearchableDescriptions(learningPath.description),
        learningPath.coverPhotoMetaUrl.flatMap(meta => converterService.asCoverPhoto(meta, imageApiClient.imageMetaOnUrl(meta)).map(_.url)),
        learningPath.duration,
        learningPath.status.toString,
        learningPath.verificationStatus.toString,
        learningPath.lastUpdated,
        asSearchableTags(learningPath.tags),
        getAuthor(learningPath.owner),
        learningPath.learningsteps.map(asSearchableLearningStep).toList,
        converterService.asApiCopyright(learningPath.copyright)
      )
    }

    def asSearchableTags(tags: Seq[LearningPathTags]): SearchableTags = {
      SearchableTags(
        nb = tags.find(_.language.contains("nb")).map(_.tags).getOrElse(Seq()),
        nn = tags.find(_.language.contains("nn")).map(_.tags).getOrElse(Seq()),
        en = tags.find(_.language.contains("en")).map(_.tags).getOrElse(Seq()),
        fr = tags.find(_.language.contains("fr")).map(_.tags).getOrElse(Seq()),
        de = tags.find(_.language.contains("de")).map(_.tags).getOrElse(Seq()),
        es = tags.find(_.language.contains("es")).map(_.tags).getOrElse(Seq()),
        se = tags.find(_.language.contains("se")).map(_.tags).getOrElse(Seq()),
        zh = tags.find(_.language.contains("zh")).map(_.tags).getOrElse(Seq()),
        unknown = tags.find(_.language.isEmpty).map(_.tags).getOrElse(Seq())
      )
    }

    def asSearchableDescriptions(description: Seq[Description]): SearchableDescriptions = {
      SearchableDescriptions(
        nb = description.find(_.language.contains(Language.NORWEGIAN_BOKMAL)).map(_.description),
        nn = description.find(_.language.contains(Language.NORWEGIAN_NYNORSK)).map(_.description),
        en = description.find(_.language.contains(Language.ENGLISH)).map(_.description),
        fr = description.find(_.language.contains(Language.FRENCH)).map(_.description),
        de = description.find(_.language.contains(Language.GERMAN)).map(_.description),
        es = description.find(_.language.contains(Language.SPANISH)).map(_.description),
        se = description.find(_.language.contains(Language.SAMI)).map(_.description),
        zh = description.find(_.language.contains(Language.CHINESE)).map(_.description),
        unknown = description.find(_.language.isEmpty).map(_.description)
      )
    }

    def asSearchableTitles(title: Seq[Title]): SearchableTitles = {
      SearchableTitles(
        nb = title.find(_.language.contains(Language.NORWEGIAN_BOKMAL)).map(_.title),
        nn = title.find(_.language.contains(Language.NORWEGIAN_NYNORSK)).map(_.title),
        en = title.find(_.language.contains(Language.ENGLISH)).map(_.title),
        fr = title.find(_.language.contains(Language.FRENCH)).map(_.title),
        de = title.find(_.language.contains(Language.GERMAN)).map(_.title),
        es = title.find(_.language.contains(Language.SPANISH)).map(_.title),
        se = title.find(_.language.contains(Language.SAMI)).map(_.title),
        zh = title.find(_.language.contains(Language.CHINESE)).map(_.title),
        unknown = title.find(_.language.isEmpty).map(_.title)
      )
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      SearchableLearningStep(
        learningStep.`type`.toString,
        asSearchableTitles(learningStep.title),
        asSearchableDescriptions(learningStep.description))
    }

    def getAuthor(owner: String): String = {
      converterService.asAuthor(authClient.getUserName(owner)).name
    }

    def asAuthor(author: String): Author = {
      Author("Forfatter", author)
    }

    def createUrlToLearningPath(id: Long): String = {
      s"${ApplicationUrl.get}$id"
    }
  }

}
