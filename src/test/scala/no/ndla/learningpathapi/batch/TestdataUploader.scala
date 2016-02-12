package no.ndla.learningpathapi.batch

import java.util.Date

import no.ndla.learningpathapi.{PropertiesLoader, LearningpathApiProperties}
import no.ndla.learningpathapi.integration.{PostgresData, AmazonIntegration}
import no.ndla.learningpathapi.model._
import org.postgresql.ds.PGPoolingDataSource


object TestdataUploader {

  val learningPaths = List(
    // KES
    LearningPath(Some(1),
    List(
      Title("Abap er et artig programmeringsspråk", Some("nb")),
      Title("Accent er et artig programmeringsspråk", Some("nn")),
      Title("Agda er et artig programmeringsspråk", Some("en"))),
    List(
      Description("Kurset dekker språket Alice.", Some("nb")),
      Description("Kurset dekker språket Argus.", Some("nn")),
      Description("The course covers the language Ada.", Some("en"))),
    Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
    120,
    LearningPathStatus.PUBLISHED,
    LearningPathVerificationStatus.CREATED_BY_NDLA,
    new Date(),
    List(LearningPathTag("ActionScript", Some("nb")), LearningPathTag("Adenine", Some("nn")), LearningPathTag("Autocoder", Some("en"))),
    "e646b7f6-60ce-4364-9e77-2a88754b95db", List(
        LearningStep(
          Some(1), Some(1), 1,
          List(Title("Læringssteg om Alef", Some("nb")), Title("Læringssteg om Able", Some("nn")), Title("Læringssteg om Absys", Some("en"))),
          List(Description("Læringssteg om Agora", Some("nb")), Description("Læringssteg om Ambienttalk", Some("nn")), Description("Læringssteg om Arexx", Some("en"))),
          List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, Some("by-nc-sa")),
        LearningStep(Some(2), Some(1), 2,
          List(Title("Læringssteg om Amos", Some("nb")), Title("Læringssteg om Babbage", Some("nn")), Title("Læringssteg om Bash", Some("en"))),
          List(Description("Læringssteg om Agilent", Some("nb")), Description("Læringssteg om Basic", Some("nn")), Description("Læringssteg om BeanShell", Some("en"))),
          List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, Some("by-nc-sa")))),

    // KES
    LearningPath(Some(2),
      List(
        Title("Bertrand er et artig programmeringsspråk", Some("nb")),
        Title("Beta er et artig programmeringsspråk", Some("nn")),
        Title("Bigwig er et artig programmeringsspråk", Some("en"))),
      List(
        Description("Kurset dekker språket Bistro.", Some("nb")),
        Description("Kurset dekker språket Bliss.", Some("nn")),
        Description("The course covers the language Blue.", Some("en"))),
      Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
      240,
      LearningPathStatus.PUBLISHED,
      LearningPathVerificationStatus.CREATED_BY_NDLA,
      new Date(),
      List(LearningPathTag("Boomerang", Some("nb")), LearningPathTag("Brew", Some("nn")), LearningPathTag("Bpel", Some("en"))),
      "e646b7f6-60ce-4364-9e77-2a88754b95db", List(
        LearningStep(
          Some(3), Some(2), 1,
          List(Title("Læringssteg om Caml", Some("nb")), Title("Læringssteg om Chef", Some("nn")), Title("Læringssteg om Chill", Some("en"))),
          List(Description("Læringssteg om Chuck", Some("nb")), Description("Læringssteg om Citrine", Some("nn")), Description("Læringssteg om Clean", Some("en"))),
          List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, Some("by-nc-sa")),
        LearningStep(Some(4), Some(2), 2,
          List(Title("Læringssteg om Clipper", Some("nb")), Title("Læringssteg om Clojure", Some("nn")), Title("Læringssteg om Cobol", Some("en"))),
          List(Description("Læringssteg om Cobra", Some("nb")), Description("Læringssteg om CoffeScript", Some("nn")), Description("Læringssteg om Coldfusion", Some("en"))),
          List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, Some("by-nc-sa")))),

    // RST
    LearningPath(Some(3),
      List(
        Title("Runes private bokmål", Some("nb")),
        Title("Runes private nynorsk", Some("nn")),
        Title("Runes private english", Some("en"))),
      List(
        Description("Kurset dekker innføring og vil gi deg grunnleggende forståelse for vanlige begrep i kunst og kultur verden. Kurset fokuserer på kunst og kultur på et verdensperspektiv.", Some("nb")),
        Description("Kurset dekker innføring og vil gje deg grunnleggjande forståing for vanlege omgrep i kunst og kultur verda. Kurset fokuserer på kunst og kultur på eit verdsperspektiv.", Some("nn")),
        Description("The course covers the introduction and will give you a basic understanding of common concepts in the arts world. The course focuses on art and culture in a world perspective", Some("en"))),
      Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
      1080,
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.EXTERNAL,
      new Date(),
      List(LearningPathTag("rune", Some("nb"))),
      "d6b2bbd0-2dd4-485a-9d9a-af2e7c9d57ad",       List(
        LearningStep(Some(5), Some(3), 1, List(Title("Tittel her", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, None),
        LearningStep(Some(6), Some(3), 2, List(Title("En annen tittel her", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, None))),

    // KW
    LearningPath(Some(4),
      List(
        Title("Kristofers private bokmål", Some("nb")),
        Title("Kristofers private nynorsk", Some("nn")),
        Title("Kristofers private english", Some("en"))),
      List(
        Description("Kurset dekker innføring og vil gi deg grunnleggende forståelse for vanlige begrep i kunst og kultur verden. Kurset fokuserer på kunst og kultur på et verdensperspektiv.", Some("nb")),
        Description("Kurset dekker innføring og vil gje deg grunnleggjande forståing for vanlege omgrep i kunst og kultur verda. Kurset fokuserer på kunst og kultur på eit verdsperspektiv.", Some("nn")),
        Description("The course covers the introduction and will give you a basic understanding of common concepts in the arts world. The course focuses on art and culture in a world perspective", Some("en"))),
      Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
      1080,
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.VERIFIED_BY_NDLA,
      new Date(),
      List(LearningPathTag("kristofer", Some("nb"))),
      "ddd2ff24-616a-484d-8172-55ddba52cd7a",
      List(
        LearningStep(Some(7), Some(4), 1, List(Title("Tittel her", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, Some("by-nc-sa")),
        LearningStep(Some(8), Some(4), 2, List(Title("En annen tittel her", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, None))),

    // Remi
    LearningPath(Some(5),
      List(
        Title("Remis private bokmål", Some("nb")),
        Title("Remis private nynorsk", Some("nn")),
        Title("Remis private english", Some("en"))),
      List(
        Description("Kurset dekker innføring og vil gi deg grunnleggende forståelse for vanlige begrep i kunst og kultur verden. Kurset fokuserer på kunst og kultur på et verdensperspektiv.", Some("nb")),
        Description("Kurset dekker innføring og vil gje deg grunnleggjande forståing for vanlege omgrep i kunst og kultur verda. Kurset fokuserer på kunst og kultur på eit verdsperspektiv.", Some("nn")),
        Description("The course covers the introduction and will give you a basic understanding of common concepts in the arts world. The course focuses on art and culture in a world perspective", Some("en"))),
      Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
      1080,
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.EXTERNAL,
      new Date(),
      List(LearningPathTag("remi", Some("nb"))),
      "a62debc3-74a7-43f3-88c9-d35837a41698",
      List(
        LearningStep(Some(9), Some(5), 1, List(Title("Tittel her", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, None),
        LearningStep(Some(10), Some(5), 2, List(Title("En annen tittel her", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, None))),

    // Christer
    LearningPath(Some(6),
      List(
        Title("Christers private bokmål", Some("nb")),
        Title("Christers private nynorsk", Some("nn")),
        Title("Christers private english", Some("en"))),
      List(
        Description("Kurset dekker innføring og vil gi deg grunnleggende forståelse for vanlige begrep i kunst og kultur verden. Kurset fokuserer på kunst og kultur på et verdensperspektiv.", Some("nb")),
        Description("Kurset dekker innføring og vil gje deg grunnleggjande forståing for vanlege omgrep i kunst og kultur verda. Kurset fokuserer på kunst og kultur på eit verdsperspektiv.", Some("nn")),
        Description("The course covers the introduction and will give you a basic understanding of common concepts in the arts world. The course focuses on art and culture in a world perspective", Some("en"))),
      Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
      1080,
      LearningPathStatus.PRIVATE,
      LearningPathVerificationStatus.CREATED_BY_NDLA,
      new Date(),
      List(LearningPathTag("christer", Some("nb"))),
      "6e74cde7-1e83-49c8-8dcd-9bbef458f477",
      List(
        LearningStep(Some(11), Some(6), 1, List(Title("Tittel her", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, None),
        LearningStep(Some(12), Some(6), 2, List(Title("En annen tittel her", Some("nb"))), List(Description("Beskrivelse", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), StepType.TEXT, None))))


  def main(args: Array[String]) {
    PropertiesLoader.load()

    val datasource = new PGPoolingDataSource()
    datasource.setUser(LearningpathApiProperties.MetaUserName)
    datasource.setPassword(LearningpathApiProperties.MetaPassword)
    datasource.setDatabaseName(LearningpathApiProperties.MetaResource)
    datasource.setServerName(LearningpathApiProperties.MetaServer)
    datasource.setPortNumber(LearningpathApiProperties.MetaPort)
    datasource.setInitialConnections(LearningpathApiProperties.MetaInitialConnections)
    datasource.setMaxConnections(LearningpathApiProperties.MetaMaxConnections)
    datasource.setCurrentSchema(LearningpathApiProperties.MetaSchema)

    val learningpathData = new PostgresData(datasource)

    learningPaths.foreach(learningpath => {
      learningpathData.exists(learningpath.id.get) match {
        case true => learningpathData.update(learningpath)
        case false => learningpathData.insert(learningpath)
      }
    })
  }
}
