/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.model.domain

object Language {
  val CHINESE = "zh"
  val ENGLISH = "en"
  val FRENCH = "fr"
  val GERMAN = "de"
  val NORWEGIAN_BOKMAL = "nb"
  val NORWEGIAN_NYNORSK = "nn"
  val SAMI = "se"
  val SPANISH = "es"
  val UNKNOWN = "unknown"

  val DefaultLanguage = NORWEGIAN_BOKMAL
  val NoLanguage = ""
  val AllLanguages = "all"

  def getLanguages[T <: Any](sequence: Seq[LanguageField[T]]): Seq[String] = {
    sequence.map(_.language.getOrElse(Language.NoLanguage)).filterNot(l => l == Language.NoLanguage)
  }

  def findSupportedLanguages(domainLearningpath: domain.LearningPath): Seq[String] = {
    val languages =
      getLanguages(domainLearningpath.title) ++
        getLanguages(domainLearningpath.description) ++
        getLanguages(domainLearningpath.tags) ++
        domainLearningpath.learningsteps.flatMap(findSupportedLanguages)

    languages.distinct
  }

  def findSupportedLanguages(domainLearningStep: domain.LearningStep): Seq[String] = {
    val languages =
      getLanguages(domainLearningStep.title) ++
        getLanguages(domainLearningStep.description) ++
        getLanguages(domainLearningStep.embedUrl)

    languages.distinct
  }

  def findByLanguage[T <: Any](sequence: Seq[LanguageField[T]], lang: String): Option[LanguageField[T]] = {
    sequence.find(_.language.getOrElse("") == lang)
  }

  def findValueByLanguage[T <: Any](sequence: Seq[LanguageField[T]], lang: String): Option[T] = {
    findByLanguage[T](sequence, lang).map(_.value)
  }

  def getByLanguageOrHead[T <: Any](sequence: Seq[LanguageField[T]], language: String): Option[T] = {
    findValueByLanguage(sequence, language) match {
      case Some(e) => Some(e)
      case None => sequence.headOption.map(lf => lf.value)
    }
  }

  def setByLanguage[T <: Any](seq: Seq[LanguageField[T]], language: String) = {
    if (language == Language.AllLanguages)
      getByLanguageOrHead[T](seq, Language.DefaultLanguage)
    else
      findValueByLanguage[T](seq, language)
  }
}

