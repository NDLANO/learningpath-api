package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.model._


class PrivateLearningpathsService {
  def learningstepFor(learningpathId: Option[String], learningstepId: Option[String]): Any = {
    Learningstep(1, List(Title("Tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT", "http://api.test.ndla.no/paths/1/learningsteps/1")
  }

  def learningstepsFor(learningpathId: Option[String]): Any = {
    List(Learningstep(1, List(Title("Tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT", "http://api.test.ndla.no/paths/1/learningsteps/1"))
  }

  def statusFor(learningPathId: Option[String]): Any = {
    LearningpathStatus("PRIVATE")
  }

  def withId(learningPathId: Option[String]): LearningpathSummary = {
    private_learningpaths.head
  }

  def all(): List[LearningpathSummary] = {
    private_learningpaths
  }

  val private_learningpaths = List(
    LearningpathSummary("3",
      List(
        Title("Skriftens historie", Some("nb")),
        Title("Historia til skrifta", Some("nn"))),
      List(
        Description("Ved skrift forstås først og fremmest tegn, der gengiver menneskelig tale sådan som fx vores alfabetiske skrift, men begrebet skrift kan også, i bredere forstand.", Some("nb")),
        Description("Ved skrift vert først forstått og fremst teikn, der gengiver menneskeleg tale såleis som fx vores alfabetiske skrift, men begrebet skrift kan òg, i breiare forstand.", Some("nn"))),
      "http://api.test.ndla.no/paths/private/3",
      1080,
      "PRIVATE",
      Author("Forfatter", "Sjonkel Rolf")),

    LearningpathSummary("4",
      List(
        Title("Musikkens historie", Some("nb")),
        Title("Historia til musikken", Some("nn"))),
      List(
        Description("Ved balbla bokmål.", Some("nb")),
        Description("Ved blabla nynorsk.", Some("nn"))),
      "http://api.test.ndla.no/paths/private/4",
      1080,
      "PRIVATE",
      Author("Forfatter", "Titten tei"))
  )
}
