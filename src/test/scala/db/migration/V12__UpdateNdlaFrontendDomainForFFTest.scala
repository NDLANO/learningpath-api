/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V12__UpdateNdlaFrontendDomainForFFTest extends UnitSuite with TestEnvironment {
  val migration = new V12__UpdateNdlaFrontendDomainForFF

  test("That ndla hosts are removed") {
    migration.updateNdlaUrl("https://ndla.no/article/1234") should be("/article/1234")
    migration.updateNdlaUrl("https://www.ndla.no/article/1234") should be("/article/1234")
    migration.updateNdlaUrl("http://ndla.no/article/1234") should be("/article/1234")
    migration.updateNdlaUrl("https://www.ndla.no/article/1234?test=true") should be("/article/1234?test=true")
    migration.updateNdlaUrl("https://ndla.no/subjects/subject:3/topic:1:179373/topic:1:170165/resource:1:16145") should be(
      "/subjects/subject:3/topic:1:179373/topic:1:170165/resource:1:16145")
    migration.updateNdlaUrl("https://ndla.no/subjects/subject:3/topic:1:179373/topic:1:170165/resource:1:16145") should be(
      "/subjects/subject:3/topic:1:179373/topic:1:170165/resource:1:16145")
  }

  test("That other urls are left untouched") {
    val urls = Seq(
      "https://www.youtube.com/watch?v=SBKvDoCqKRc",
      "https://vimeo.com/110553311",
      "https://www.ted.com/talks/danny_hillis_exploring_options_for_solar_geoengineering"
    )

    urls.foreach(url => migration.updateNdlaUrl(url) should be(url))
  }

}
