/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service.search

import no.ndla.learningpathapi.model.domain.{Description, LearningPathTags, Title}
import no.ndla.learningpathapi.model.search.{SearchableDescriptions, SearchableLearningStep, SearchableTitles}
import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {

  var service: SearchConverterService = _

  override def beforeEach: Unit = {
    service = new SearchConverterService
  }

  test("That asApiTitle returns all titles") {
    val searchableTitle = SearchableTitles(
      nb = Some("Bokmål"),
      nn = Some("Nynorsk"),
      en = Some("Engelsk"),
      fr = Some("Fransk"),
      de = Some("Tysk"),
      es = Some("Spansk"),
      se = Some("Samisk"),
      zh = Some("Kinesisk"),
      unknown = Some("Ukjent")
    )

    val titleList = service.asApiTitle(searchableTitle)
    titleList.size should be(9)
    titleList.find(_.language == "nb").map(_.title) should equal(Some("Bokmål"))
    titleList.find(_.language == "nn").map(_.title) should equal(Some("Nynorsk"))
    titleList.find(_.language == "en").map(_.title) should equal(Some("Engelsk"))
    titleList.find(_.language == "fr").map(_.title) should equal(Some("Fransk"))
    titleList.find(_.language == "de").map(_.title) should equal(Some("Tysk"))
    titleList.find(_.language == "es").map(_.title) should equal(Some("Spansk"))
    titleList.find(_.language == "se").map(_.title) should equal(Some("Samisk"))
    titleList.find(_.language == "zh").map(_.title) should equal(Some("Kinesisk"))
    titleList.find(_.language == "unknown").map(_.title) should equal(Some("Ukjent"))

  }

  test("That asApiTitle does not return empty titles for a supported language") {
    val searchableTitle = SearchableTitles(nb = Some("Tittel"),
                                           nn = None,
                                           en = None,
                                           fr = None,
                                           de = None,
                                           es = None,
                                           se = None,
                                           zh = None,
                                           unknown = None)
    val titleList = service.asApiTitle(searchableTitle)
    titleList.size should be(1)
    titleList.head.title should equal("Tittel")
    titleList.head.language should equal("nb")
  }

  test("That asSearchableTitles converts language to correct place") {
    val searchableTitles = service.asSearchableTitles(List(Title("Tittel", "nb"), Title("Title", "en")))

    searchableTitles.nb should equal(Some("Tittel"))
    searchableTitles.en should equal(Some("Title"))
    searchableTitles.de should be(None)
    searchableTitles.nn should be(None)
    searchableTitles.es should be(None)
    searchableTitles.zh should be(None)
    searchableTitles.fr should be(None)
    searchableTitles.se should be(None)
    searchableTitles.unknown should be(None)
  }

  test("That asApiDescription returns all descriptions") {
    val searchableDescriptions = SearchableDescriptions(
      nb = Some("Bokmål"),
      nn = Some("Nynorsk"),
      en = Some("Engelsk"),
      fr = Some("Fransk"),
      de = Some("Tysk"),
      es = Some("Spansk"),
      se = Some("Samisk"),
      zh = Some("Kinesisk"),
      unknown = Some("Ukjent")
    )

    val descriptionList = service.asApiDescription(searchableDescriptions)
    descriptionList.size should be(9)
    descriptionList.find(_.language == "nb").map(_.description) should equal(Some("Bokmål"))
    descriptionList.find(_.language == "nn").map(_.description) should equal(Some("Nynorsk"))
    descriptionList.find(_.language == "en").map(_.description) should equal(Some("Engelsk"))
    descriptionList.find(_.language == "fr").map(_.description) should equal(Some("Fransk"))
    descriptionList.find(_.language == "de").map(_.description) should equal(Some("Tysk"))
    descriptionList.find(_.language == "es").map(_.description) should equal(Some("Spansk"))
    descriptionList.find(_.language == "se").map(_.description) should equal(Some("Samisk"))
    descriptionList.find(_.language == "zh").map(_.description) should equal(Some("Kinesisk"))
    descriptionList
      .find(_.language == "unknown")
      .map(_.description) should equal(Some("Ukjent"))

  }

  test("That asApiDescription does not return empty titles for a supported language") {
    val searchableDescriptions =
      SearchableDescriptions(nb = Some("Beskrivelse"),
                             nn = None,
                             en = None,
                             fr = None,
                             de = None,
                             es = None,
                             se = None,
                             zh = None,
                             unknown = None)
    val descriptionList = service.asApiDescription(searchableDescriptions)
    descriptionList.size should be(1)
    descriptionList.head.description should equal("Beskrivelse")
    descriptionList.head.language should equal("nb")
  }

  test("That asSearchableDescriptions converts language to correct place") {
    val searchableDescriptions =
      service.asSearchableDescriptions(List(Description("Beskrivelse", "nb"), Description("Description", "en")))

    searchableDescriptions.nb should equal(Some("Beskrivelse"))
    searchableDescriptions.en should equal(Some("Description"))
    searchableDescriptions.de should be(None)
    searchableDescriptions.nn should be(None)
    searchableDescriptions.es should be(None)
    searchableDescriptions.zh should be(None)
    searchableDescriptions.fr should be(None)
    searchableDescriptions.se should be(None)
    searchableDescriptions.unknown should be(None)
  }

  test("That tags converts to correct place") {
    val searchableTags = service.asSearchableTags(
      List(
        LearningPathTags(Seq("Tag1", "Tag2"), "nb"),
        LearningPathTags(Seq("Tagg1", "Tagg2"), "nn"),
        LearningPathTags(Seq("Los Taggos1", "Los Taggos2"), "es"),
        LearningPathTags(Seq("Lasdf adf"), "unknown")
      ))

    searchableTags.nb should equal(Some(Seq("Tag1", "Tag2")))
    searchableTags.nn should equal(Some(Seq("Tagg1", "Tagg2")))
    searchableTags.es should equal(Some(Seq("Los Taggos1", "Los Taggos2")))
    searchableTags.unknown should equal(Some(Seq("Lasdf adf")))
    searchableTags.de should equal(None)
    searchableTags.en should equal(None)
    searchableTags.fr should equal(None)
    searchableTags.se should equal(None)
    searchableTags.zh should equal(None)
  }

  test("That asApiIntroduction returns the step as an introduction in all available languages") {
    val searchableTitles =
      SearchableTitles(None, None, None, None, None, None, None, None, None)
    val searchableDescriptions = SearchableDescriptions(
      nb = Some("Bokmål"),
      nn = Some("Nynorsk"),
      en = Some("English"),
      None,
      None,
      None,
      None,
      None,
      None
    )
    val embedUrls = List("https://ndla.no/article/123")
    val learningStep = SearchableLearningStep("INTRODUCTION", embedUrls, searchableTitles, searchableDescriptions)

    val apiIntroductions = service.asApiIntroduction(Some(learningStep))
    apiIntroductions.size should be(3)
    apiIntroductions.find(_.language == "nb").map(_.introduction) should be(Some("Bokmål"))
    apiIntroductions.find(_.language == "nn").map(_.introduction) should be(Some("Nynorsk"))
    apiIntroductions.find(_.language == "en").map(_.introduction) should be(Some("English"))
  }

  test("That asApiIntroduction returns no introduction if no descriptions are available") {
    val searchableTitles =
      SearchableTitles(None, None, None, None, None, None, None, None, None)
    val searchableDescriptions = SearchableDescriptions(None, None, None, None, None, None, None, None, None)
    val embedUrls = List("https://ndla.no/article/123")
    val learningStep = SearchableLearningStep("INTRODUCTION", embedUrls, searchableTitles, searchableDescriptions)

    val apiIntroductions = service.asApiIntroduction(Some(learningStep))
    apiIntroductions.size should be(0)
  }

  test("That asApiIntroduction returns an empty list for None") {
    service.asApiIntroduction(None) should equal(List())
  }

}
