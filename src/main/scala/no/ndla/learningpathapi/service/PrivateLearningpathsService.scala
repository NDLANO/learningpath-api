package no.ndla.learningpathapi.service

import java.util.Date
import no.ndla.learningpathapi.model._


class PrivateLearningpathsService {
  def learningstepFor(learningpathId: Option[String], learningstepId: Option[String]): Any = {
    learningstep1
  }

  def learningstepsFor(learningpathId: Option[String]): Any = {
    List(learningstep1, learningstep2)
  }

  def statusFor(learningPathId: Option[String]): Any = {
    LearningpathStatus("PRIVATE")
  }

  def withId(learningPathId: Option[String]): Learningpath = {
    detailed_learningpath
  }

  def all(): List[LearningpathSummary] = {
    private_learningpaths
  }

  val learningstep1 = Learningstep(1, 1, List(Title("Tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT", "http://api.test.ndla.no/paths/3/learningsteps/1")
  val learningstep2 = Learningstep(2, 2, List(Title("En annen tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT", "http://api.test.ndla.no/paths/3/learningsteps/2")

  val detailed_learningpath = Learningpath(
    "1",
    List(
      Title("Hva er kunst og kultur?", Some("nb")),
      Title("Kva er kunst og kultur?", Some("nn")),
      Title("What is art and culture?", Some("en"))),
    List(
      Description("Kurset dekker innføring og vil gi deg grunnleggende forståelse for vanlige begrep i kunst og kultur verden. Kurset fokuserer på kunst og kultur på et verdensperspektiv.", Some("nb")),
      Description("Kurset dekker innføring og vil gje deg grunnleggjande forståing for vanlege omgrep i kunst og kultur verda. Kurset fokuserer på kunst og kultur på eit verdsperspektiv.", Some("nn")),
      Description("The course covers the introduction and will give you a basic understanding of common concepts in the arts world. The course focuses on art and culture in a world perspective", Some("en"))),
    List(learningstep1, learningstep2),
    "http://api.test.ndla.no/paths/private/3/learningsteps",
    "http://api.test.ndla.no/paths/private/3",
    Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
    1080,
    "PUBLISHED",
    new Date(),
    Author("Forfatter", "Kaptein Sabeltann")
  )

  val private_learningpaths = List(
    LearningpathSummary("3",
      List(
        Title("Skriftens historie", Some("nb")),
        Title("Historia til skrifta", Some("nn"))),
      List(
        Description("Ved skrift forstås først og fremmest tegn, der gengiver menneskelig tale sådan som fx vores alfabetiske skrift, men begrebet skrift kan også, i bredere forstand.", Some("nb")),
        Description("Ved skrift vert først forstått og fremst teikn, der gengiver menneskeleg tale såleis som fx vores alfabetiske skrift, men begrebet skrift kan òg, i breiare forstand.", Some("nn"))),
      "http://api.test.ndla.no/paths/private/3",
      Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
      1080,
      "PRIVATE",
      new Date(),
      Author("Forfatter", "Sjonkel Rolf")),

    LearningpathSummary("4",
      List(
        Title("Musikkens historie", Some("nb")),
        Title("Historia til musikken", Some("nn"))),
      List(
        Description("Ved balbla bokmål.", Some("nb")),
        Description("Ved blabla nynorsk.", Some("nn"))),
      "http://api.test.ndla.no/paths/private/4",
      Some("http://api.ndla.no/images/full/sy2fe75b.jpg"),
      1080,
      "PRIVATE",
      new Date(),
      Author("Forfatter", "Titten tei"))
  )
}
