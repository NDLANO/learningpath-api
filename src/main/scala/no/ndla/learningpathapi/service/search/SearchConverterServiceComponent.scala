/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import com.sksamuel.elastic4s.http.search.SearchHit
import no.ndla.learningpathapi.LearningpathApiProperties.DefaultLanguage
import no.ndla.learningpathapi.integration.ImageApiClientComponent
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.api.{Author, LearningPathSummaryV2, SearchResultV2}
import no.ndla.learningpathapi.model.domain.Language.findByLanguageOrBestEffort
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.search._
import no.ndla.learningpathapi.service.ConverterService
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl

trait SearchConverterServiceComponent {
  this: ConverterService with ImageApiClientComponent =>
  val searchConverterService: SearchConverterService

  class SearchConverterService {

    def asApiLearningPathSummaryV2(searchableLearningPath: SearchableLearningPath,
                                   language: String): LearningPathSummaryV2 = {
      val titles = searchableLearningPath.titles.languageValues.map(lv => api.Title(lv.value, lv.language))
      val descriptions =
        searchableLearningPath.descriptions.languageValues.map(lv => api.Description(lv.value, lv.language))
      val introductions =
        searchableLearningPath.learningsteps.find(_.stepType == StepType.INTRODUCTION.toString) match {
          case Some(step) => step.descriptions.languageValues.map(lv => api.Introduction(lv.value, lv.language))
          case _          => Seq.empty
        }
      val tags = searchableLearningPath.tags.languageValues.map(lv => api.LearningPathTags(lv.value, lv.language))
      val supportedLanguages = Language.findSupportedLanguages(titles, descriptions, introductions, tags)

      LearningPathSummaryV2(
        searchableLearningPath.id,
        revision = None,
        findByLanguageOrBestEffort(titles, language)
          .getOrElse(api.Title("", DefaultLanguage)),
        findByLanguageOrBestEffort(descriptions, language)
          .getOrElse(api.Description("", DefaultLanguage)),
        findByLanguageOrBestEffort(introductions, language)
          .getOrElse(api.Introduction("", DefaultLanguage)),
        createUrlToLearningPath(searchableLearningPath.id),
        searchableLearningPath.coverPhotoUrl,
        searchableLearningPath.duration,
        searchableLearningPath.status,
        searchableLearningPath.lastUpdated,
        findByLanguageOrBestEffort(tags, language)
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
            Language.languageAnalyzers.map(la => la.languageTag.toString).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableLearningPath(
        learningPath.id.get,
        SearchableLanguageValues(learningPath.title.map(title => LanguageValue(title.language, title.title))),
        SearchableLanguageValues(learningPath.description.map(desc => LanguageValue(desc.language, desc.description))),
        learningPath.coverPhotoId
          .flatMap(converterService.asCoverPhoto)
          .map(_.url),
        learningPath.duration,
        learningPath.status.toString,
        learningPath.verificationStatus.toString,
        learningPath.lastUpdated,
        defaultTitle.map(_.title),
        SearchableLanguageList(learningPath.tags.map(tags => LanguageValue(tags.language, tags.tags))),
        learningPath.learningsteps.getOrElse(Seq.empty).map(asSearchableLearningStep).toList,
        converterService.asApiCopyright(learningPath.copyright),
        learningPath.isBasedOn
      )
    }

    def asSearchableLearningStep(learningStep: LearningStep): SearchableLearningStep = {
      SearchableLearningStep(
        learningStep.`type`.toString,
        learningStep.embedUrl.map(_.url).toList,
        learningStep.status.toString,
        SearchableLanguageValues(learningStep.title.map(title => LanguageValue(title.language, title.title))),
        SearchableLanguageValues(learningStep.description.map(desc => LanguageValue(desc.language, desc.description))),
      )
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
