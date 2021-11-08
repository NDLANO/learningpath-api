/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.learningpathapi.{TestEnvironment, UnitSuite}

class V14__ConvertLanguageUnknownTest extends UnitSuite with TestEnvironment {
  val migration = new V14__ConvertLanguageUnknown

  test("Language unknown is replaced with und for learningpath") {
    val original =
      """{"tags":[{"tags":["etos","logos","patos","retorikk","talekunst"],"language":"unknown"}],"owner":"eo7SHvVg9_h_gvOnVgn1hNYN","title":[{"title":"Etos, patos og logos","language":"unknown"}],"status":"PRIVATE","duration":180,"copyright":{"license":"CC-BY-SA-4.0","contributors":[{"name":"Elin Marie Voie","type":"Redaksjonelt"}]},"isBasedOn":723,"description":[{"description":"En enkel innføring i retorikk for elever i yrkesfag og studiespesialiserende Vg1.","language":"unknown"}],"lastUpdated":"2019-08-21T13:51:38Z","coverPhotoId":"3683","verificationStatus":"EXTERNAL"}"""
    val expected =
      """{"tags":[{"tags":["etos","logos","patos","retorikk","talekunst"],"language":"und"}],"owner":"eo7SHvVg9_h_gvOnVgn1hNYN","title":[{"title":"Etos, patos og logos","language":"und"}],"status":"PRIVATE","duration":180,"copyright":{"license":"CC-BY-SA-4.0","contributors":[{"name":"Elin Marie Voie","type":"Redaksjonelt"}]},"isBasedOn":723,"description":[{"description":"En enkel innføring i retorikk for elever i yrkesfag og studiespesialiserende Vg1.","language":"und"}],"lastUpdated":"2019-08-21T13:51:38Z","coverPhotoId":"3683","verificationStatus":"EXTERNAL"}"""

    migration.convertLearningPathDocument(original) should be(expected)
  }

  test("Language unknown is replaced with und for learningstep") {
    val original =
      """{"type":"TEXT","seqNo":1,"title":[{"title":"Prisindeks. Konsumprisindeks","language":"unknown"}],"status":"ACTIVE","license":"","embedUrl":[{"url":"/article-iframe/nn/urn:resource:1:48049/7539","language":"unknown","embedType":"iframe"}],"showTitle":false,"description":[{"description":"Desc","language":"unknown"}],"learningPathId":427}"""
    val expected =
      """{"type":"TEXT","seqNo":1,"title":[{"title":"Prisindeks. Konsumprisindeks","language":"und"}],"status":"ACTIVE","license":"","embedUrl":[{"url":"/article-iframe/nn/urn:resource:1:48049/7539","language":"und","embedType":"iframe"}],"showTitle":false,"description":[{"description":"Desc","language":"und"}],"learningPathId":427}"""

    migration.convertLearningStepDocument(original) should be(expected)
  }
}
