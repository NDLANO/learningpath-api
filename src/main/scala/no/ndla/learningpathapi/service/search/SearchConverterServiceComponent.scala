/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import com.sksamuel.elastic4s.http.search.SearchHit
import no.ndla.learningpathapi.integration.ImageApiClientComponent
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.api.{Author, Introduction, LearningPathSummaryV2, SearchResultV2}
import no.ndla.learningpathapi.model.domain.Language.{DefaultLanguage, findByLanguageOrBestEffort}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.search._
import no.ndla.learningpathapi.service.ConverterService
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl

trait SearchConverterServiceComponent {
  this: ConverterService with ImageApiClientComponent =>
  val searchConverterService: SearchConverterService

  class SearchConverterService {

    def asApiTitle(titles: SearchableTitles): List[api.Title] = {
      List(
        (titles.zh, Language.CHINESE),
        (titles.en, Language.ENGLISH),
        (titles.fr, Language.FRENCH),
        (titles.de, Language.GERMAN),
        (titles.nb, Language.NORWEGIAN_BOKMAL),
        (titles.nn, Language.NORWEGIAN_NYNORSK),
        (titles.se, Language.SAMI),
        (titles.es, Language.SPANISH),
        (titles.unknown, Language.UNKNOWN)
      ).filter(_._1.isDefined).map(tuple => api.Title(tuple._1.get, tuple._2))
    }

    def asApiDescription(descriptions: SearchableDescriptions): Seq[api.Description] = {
      List(
        (descriptions.zh, Language.CHINESE),
        (descriptions.en, Language.ENGLISH),
        (descriptions.fr, Language.FRENCH),
        (descriptions.de, Language.GERMAN),
        (descriptions.nb, Language.NORWEGIAN_BOKMAL),
        (descriptions.nn, Language.NORWEGIAN_NYNORSK),
        (descriptions.se, Language.SAMI),
        (descriptions.es, Language.SPANISH),
        (descriptions.unknown, Language.UNKNOWN)
      ).filter(_._1.isDefined)
        .map(tuple => api.Description(tuple._1.get, tuple._2))
    }

    def asApiLearningPathTag(tags: SearchableTags): Seq[api.LearningPathTags] = {
      Seq(
        api.LearningPathTags(tags.zh.getOrElse(Seq.empty), Language.CHINESE),
        api.LearningPathTags(tags.en.getOrElse(Seq.empty), Language.ENGLISH),
        api.LearningPathTags(tags.fr.getOrElse(Seq.empty), Language.FRENCH),
        api.LearningPathTags(tags.de.getOrElse(Seq.empty), Language.GERMAN),
        api.LearningPathTags(tags.nb.getOrElse(Seq.empty), Language.NORWEGIAN_BOKMAL),
        api.LearningPathTags(tags.nn.getOrElse(Seq.empty), Language.NORWEGIAN_NYNORSK),
        api.LearningPathTags(tags.se.getOrElse(Seq.empty), Language.SAMI),
        api.LearningPathTags(tags.es.getOrElse(Seq.empty), Language.SPANISH),
        api.LearningPathTags(tags.unknown.getOrElse(Seq.empty), Language.UNKNOWN)
      ).filterNot(_.tags.isEmpty)
    }

    def asApiIntroduction(learningStep: Option[SearchableLearningStep]): Seq[Introduction] = {
      learningStep.map(_.descriptions) match {
        case None => List()
        case Some(descriptions) =>
          List(
            (descriptions.zh, Language.CHINESE),
            (descriptions.en, Language.ENGLISH),
            (descriptions.fr, Language.FRENCH),
            (descriptions.de, Language.GERMAN),
            (descriptions.nb, Language.NORWEGIAN_BOKMAL),
            (descriptions.nn, Language.NORWEGIAN_NYNORSK),
            (descriptions.se, Language.SAMI),
            (descriptions.es, Language.SPANISH),
            (descriptions.unknown, Language.UNKNOWN)
          ).filter(_._1.isDefined)
            .map(tuple => Introduction(tuple._1.get, tuple._2))
      }
    }

    def asApiLearningPathSummaryV2(searchableLearningPath: SearchableLearningPath,
                                   language: String): LearningPathSummaryV2 = {
      val titles = asApiTitle(searchableLearningPath.titles)
      val descriptions = asApiDescription(searchableLearningPath.descriptions)
      val introductions = asApiIntroduction(
        searchableLearningPath.learningsteps.find(_.stepType == StepType.INTRODUCTION.toString))
      val tags = asApiLearningPathTag(searchableLearningPath.tags)
      val supportedLanguages = Language.findSupportedLanguages(titles, descriptions, introductions, tags)

      LearningPathSummaryV2(
        searchableLearningPath.id,
        revision = None,
        findByLanguageOrBestEffort(titles, Some(language))
          .getOrElse(api.Title("", DefaultLanguage)),
        findByLanguageOrBestEffort(descriptions, Some(language))
          .getOrElse(api.Description("", DefaultLanguage)),
        findByLanguageOrBestEffort(introductions, Some(language))
          .getOrElse(api.Introduction("", DefaultLanguage)),
        createUrlToLearningPath(searchableLearningPath.id),
        searchableLearningPath.coverPhotoUrl,
        searchableLearningPath.duration,
        searchableLearningPath.status,
        searchableLearningPath.lastUpdated,
        findByLanguageOrBestEffort(tags, Some(language))
          .getOrElse(api.LearningPathTags(Seq(), DefaultLanguage)),
        searchableLearningPath.copyright,
        supportedLanguages,
        searchableLearningPath.isBasedOn,
        message = None
      )
    }

    def asSearchableLearningpath(learningPath: LearningPath): SearchableLearningPath = {
      val defaultTitle = learningPath.title
        .sortBy(title => {
          val languagePriority =
            Language.languageAnalyzers.map(la => la.lang).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableLearningPath(
        learningPath.id.get,
        asSearchableTitles(learningPath.title),
        asSearchableDescriptions(learningPath.description),
        learningPath.coverPhotoId
          .flatMap(converterService.asCoverPhoto)
          .map(_.url),
        learningPath.duration,
        learningPath.status.toString,
        learningPath.verificationStatus.toString,
        learningPath.lastUpdated,
        defaultTitle.map(_.title),
        asSearchableTags(learningPath.tags),
        learningPath.learningsteps.map(asSearchableLearningStep).toList,
        converterService.asApiCopyright(learningPath.copyright),
        learningPath.isBasedOn
      )
    }

    def asSearchableTags(tags: Seq[LearningPathTags]): SearchableTags = {
      SearchableTags(
        nb = tags.find(_.language == Language.NORWEGIAN_BOKMAL).map(_.tags),
        nn = tags.find(_.language == Language.NORWEGIAN_NYNORSK).map(_.tags),
        en = tags.find(_.language == Language.ENGLISH).map(_.tags),
        fr = tags.find(_.language == Language.FRENCH).map(_.tags),
        de = tags.find(_.language == Language.GERMAN).map(_.tags),
        es = tags.find(_.language == Language.SPANISH).map(_.tags),
        se = tags.find(_.language == Language.SAMI).map(_.tags),
        zh = tags.find(_.language == Language.CHINESE).map(_.tags),
        unknown = tags.find(_.language == Language.UNKNOWN).map(_.tags)
      )
    }

    def asSearchableDescriptions(description: Seq[Description]): SearchableDescriptions = {
      SearchableDescriptions(
        nb = description
          .find(_.language == Language.NORWEGIAN_BOKMAL)
          .map(_.description),
        nn = description
          .find(_.language == Language.NORWEGIAN_NYNORSK)
          .map(_.description),
        en = description.find(_.language == Language.ENGLISH).map(_.description),
        fr = description.find(_.language == Language.FRENCH).map(_.description),
        de = description.find(_.language == Language.GERMAN).map(_.description),
        es = description.find(_.language == Language.SPANISH).map(_.description),
        se = description.find(_.language == Language.SAMI).map(_.description),
        zh = description.find(_.language == Language.CHINESE).map(_.description),
        unknown = description.find(_.language == Language.UNKNOWN).map(_.description)
      )
    }

    def asSearchableTitles(title: Seq[Title]): SearchableTitles = {
      SearchableTitles(
        nb = title.find(_.language == Language.NORWEGIAN_BOKMAL).map(_.title),
        nn = title.find(_.language == Language.NORWEGIAN_NYNORSK).map(_.title),
        en = title.find(_.language == Language.ENGLISH).map(_.title),
        fr = title.find(_.language == Language.FRENCH).map(_.title),
        de = title.find(_.language == Language.GERMAN).map(_.title),
        es = title.find(_.language == Language.SPANISH).map(_.title),
        se = title.find(_.language == Language.SAMI).map(_.title),
        zh = title.find(_.language == Language.CHINESE).map(_.title),
        unknown = title.find(_.language == Language.UNKNOWN).map(_.title)
      )
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      SearchableLearningStep(learningStep.`type`.toString,
                             asSearchableTitles(learningStep.title),
                             asSearchableDescriptions(learningStep.description))
    }

    def asAuthor(author: String): Author = {
      Author("Forfatter", author)
    }

    def createUrlToLearningPath(id: Long): String = {
      s"${ApplicationUrl.get}$id"
    }

    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keyLanguages = keys.toList.flatMap(key =>
          key.split('.').toList match {
            case _ :: language :: _ => Some(language)
            case _                  => None
        })

        keyLanguages
          .sortBy(lang => {
            ISO639.languagePriority.reverse.indexOf(lang)
          })
          .lastOption
      }

      val highlightKeys: Option[Map[String, _]] = Option(result.highlight)
      val matchLanguage = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          keyToLanguage(result.sourceAsMap.keys)
      }
    }

    def asApiSearchResult(searchResult: SearchResult): SearchResultV2 =
      SearchResultV2(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

  }

}
