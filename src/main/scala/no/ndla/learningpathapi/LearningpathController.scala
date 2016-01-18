package no.ndla.learningpathapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.model._
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}

class LearningpathController(implicit val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats + new EnumNameSerializer(PublishState)

  protected val applicationDescription = "API for accessing Learningpaths from ndla.no."
  val getLearningpaths =
    (apiOperation[List[LearningpathSummary]]("getLearningpaths")
      summary "Show all public learningpaths"
      notes "Shows all the public learningpaths."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
      queryParam[Option[String]]("query").description("Return only Learningpaths's with content matching the specified query."),
      queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
      queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
      queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
      )
      )

  val getLearningpath =
    (apiOperation[Learningpath]("getLearningpath")
      summary "Show details about the specified public learningpath"
      notes "Shows all information about the specified learningpath."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
      queryParam[Option[String]]("learningpath_id").description("The id of the learningpath.")
      )
      )

  val getPrivateLearningpaths =
    (apiOperation[List[LearningpathSummary]]("getPrivateLearningpaths")
      summary "Show all learningpaths owned by the logged in user."
      notes "Shows all the learningpaths owned by the logged in user."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("query").description("Return only Learningpaths's with content matching the specified query."),
      queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
      queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
      queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
      )
      )

  val getPrivateLearningpath =
    (apiOperation[Learningpath]("getPrivateLearningpath")
      summary "Show details about the specified learningpath owned by the logged in user."
      notes "Shows all information about the specified learningpath owned by the logged in user.."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      headerParam[Option[String]]("app-key").description("Your app-key."),
      queryParam[Option[String]]("learningpath_id").description("The id of the learningpath.")
      )
      )

  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
  }

  after() {
    LoggerContext.clearCorrelationID
    ApplicationUrl.clear()
  }

  get("/", operation(getLearningpaths)) {
    public_learningpaths
  }

  get("/:learningpath_id", operation(getLearningpath)) {
    public_learningpaths.head
  }

  get("/private/", operation(getPrivateLearningpaths)) {
    private_learningpaths
  }

  get ("/private/:learningpath_id", operation(getPrivateLearningpath)){
    private_learningpaths.head
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
    "http://api.test.ndla.no/learningpaths/1",
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
      "http://api.test.ndla.no/learningpaths/2",
      1080,
      "PUBLISHED",
      Author("Forfatter", "Kaptein Sabeltann"))
  )

  val private_learningpaths = List(
    LearningpathSummary("3",
      List(
        Title("Skriftens historie", Some("nb")),
        Title("Historia til skrifta", Some("nn"))),
      List(
        Description("Ved skrift forstås først og fremmest tegn, der gengiver menneskelig tale sådan som fx vores alfabetiske skrift, men begrebet skrift kan også, i bredere forstand.", Some("nb")),
        Description("Ved skrift vert først forstått og fremst teikn, der gengiver menneskeleg tale såleis som fx vores alfabetiske skrift, men begrebet skrift kan òg, i breiare forstand.", Some("nn"))),
      "http://api.test.ndla.no/learningpaths/private/3",
      1080,
      "PRIVATE",
      Author("Forfatter", "Sjonkel Rolf"))
  )
}
