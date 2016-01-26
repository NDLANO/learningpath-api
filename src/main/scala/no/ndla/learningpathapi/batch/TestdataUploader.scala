package no.ndla.learningpathapi.batch

import java.util.Date

import no.ndla.learningpathapi.LearningpathApiProperties
import no.ndla.learningpathapi.integration.AmazonIntegration
import no.ndla.learningpathapi.model._


object TestdataUploader {

  val testdata = List(
    LearningPath(Some(1),
    List(
      Title("Hva er kunst og kultur?", Some("nb")),
      Title("Kva er kunst og kultur?", Some("nn")),
      Title("What is art and culture?", Some("en"))),
    List(
      Description("Kurset dekker innføring og vil gi deg grunnleggende forståelse for vanlige begrep i kunst og kultur verden. Kurset fokuserer på kunst og kultur på et verdensperspektiv.", Some("nb")),
      Description("Kurset dekker innføring og vil gje deg grunnleggjande forståing for vanlege omgrep i kunst og kultur verda. Kurset fokuserer på kunst og kultur på eit verdsperspektiv.", Some("nn")),
      Description("The course covers the introduction and will give you a basic understanding of common concepts in the arts world. The course focuses on art and culture in a world perspective", Some("en"))),
    List(
      LearningStep(1, 1, List(Title("Tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT"),
      LearningStep(2, 2, List(Title("En annen tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT")),
    Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
    1080,
    LearningpathApiProperties.Published,
    new Date(),
    "kes-kes-kes"),
    LearningPath(Some(2),
      List(
        Title("Hva er kunst og kultur?", Some("nb")),
        Title("Kva er kunst og kultur?", Some("nn")),
        Title("What is art and culture?", Some("en"))),
      List(
        Description("Kurset dekker innføring og vil gi deg grunnleggende forståelse for vanlige begrep i kunst og kultur verden. Kurset fokuserer på kunst og kultur på et verdensperspektiv.", Some("nb")),
        Description("Kurset dekker innføring og vil gje deg grunnleggjande forståing for vanlege omgrep i kunst og kultur verda. Kurset fokuserer på kunst og kultur på eit verdsperspektiv.", Some("nn")),
        Description("The course covers the introduction and will give you a basic understanding of common concepts in the arts world. The course focuses on art and culture in a world perspective", Some("en"))),
      List(
        LearningStep(1, 1, List(Title("Tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT"),
        LearningStep(2, 2, List(Title("En annen tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT")),
      Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
      1080,
      LearningpathApiProperties.Private,
      new Date(),
      "kes-kes-kes"))


  def main(args: Array[String]) {
    val learningpathData = AmazonIntegration.getLearningpathData()

    testdata.foreach(learningpath => {
      learningpathData.exists(learningpath) match {
        case true => learningpathData.update(learningpath, "kes-kes-kes")
        case false => learningpathData.insert(learningpath, "kes-kes-kes")
      }
    })
  }
}
