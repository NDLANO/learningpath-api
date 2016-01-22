package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.model._


class PublicLearningpathsService {
  def learningstepFor(learningpathId: Option[String], learningstepId: Option[String]): Any = {
    Learningstep(1, List(Title("Tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT", "http://api.test.ndla.no/paths/1/learningsteps/1")
  }

  def learningstepsFor(learningpathId: Option[String]): List[Learningstep] = {
    List(Learningstep(1, List(Title("Tittel her", Some("nb"))), List(EmbedUrl("http://www.vg.no", Some("nb"))), "TEXT", "http://api.test.ndla.no/paths/1/learningsteps/1"))
  }

  def statusFor(learningPathId: Option[String]): Any = {
    LearningpathStatus("PUBLISHED")
  }

  def withId(learningPathId: Option[String]): LearningpathSummary = {
    public_learningpaths.head
  }

  def all(): List[LearningpathSummary] = {
    public_learningpaths
  }


  val public_learningpaths = List(
    LearningpathSummary("1",
      List(
        Title("Hva er kunst og kultur?", Some("nb")),
        Title("Kva er kunst og kultur?", Some("nn")),
        Title("What is art and culture?", Some("en"))),
      List(
        Description("Kurset dekker innføring og vil gi deg grunnleggende forståelse for vanlige begrep i kunst og kultur verden. Kurset fokuserer på kunst og kultur på et verdensperspektiv.", Some("nb")),
        Description("Kurset dekker innføring og vil gje deg grunnleggjande forståing for vanlege omgrep i kunst og kultur verda. Kurset fokuserer på kunst og kultur på eit verdsperspektiv.", Some("nn")),
        Description("The course covers the introduction and will give you a basic understanding of common concepts in the arts world. The course focuses on art and culture in a world perspective", Some("en"))),
      "http://api.test.ndla.no/paths/1",
      1080,
      "PUBLISHED",
      Author("Forfatter", "Snurre Sprett")),

    LearningpathSummary("2",
      List(
        Title("Leselighet og skrift", Some("nb")),
        Title("Leselighet og skrift", Some("nn"))),
      List(
        Description("Uttrykkene \"leselighet\" og \"lesbarhet\" brukes om hverandre i norsk fagterminologi, og ofte uten klare forestillinger om hva begrepene står for.", Some("nb")),
        Description("Uttrykka \"leselighet\" og \"lesbarhet\" vert brukt om kvarandre i norsk fagterminologi, og ofte utan klåre førestillingar om kva omgrepa står for.", Some("nn"))),
      "http://api.test.ndla.no/paths/2",
      1080,
      "PUBLISHED",
      Author("Forfatter", "Kaptein Sabeltann"))
  )

}
