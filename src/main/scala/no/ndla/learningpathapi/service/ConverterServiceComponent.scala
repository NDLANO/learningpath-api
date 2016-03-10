package no.ndla.learningpathapi.service

import no.ndla.learningpathapi
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.integration.{OEmbedClientComponent, AuthClientComponent}
import no.ndla.learningpathapi.model.{LearningStep, LearningPath, NdlaUserName}
import no.ndla.network.ApplicationUrl

trait ConverterServiceComponent {
  this: AuthClientComponent with OEmbedClientComponent =>
  val converterService: ConverterService

  class ConverterService {
    def asEmbedUrl(embedContent: EmbedContent): model.EmbedUrl = {
      model.EmbedUrl(embedContent.url, embedContent.language)
    }

    def asDescription(description: Description): model.Description = {
      model.Description(description.description, description.language)
    }

    def asTitle(title: Title): model.Title = {
      model.Title(title.title, title.language)
    }


    def asLearningPathTag(tag: LearningPathTag): model.LearningPathTag = {
      model.LearningPathTag(tag.tag, tag.language)
    }

    def asApiLearningPathTag(tag: model.LearningPathTag): LearningPathTag = {
      LearningPathTag(tag.tag, tag.language)
    }

    def asAuthor(user: NdlaUserName): Author = {
      val names = Array(user.first_name, user.middle_name, user.last_name).filter(_.isDefined).map(_.get)
      Author("Forfatter", names.mkString(" "))
    }

    def asApiLearningpath(lp: LearningPath, callOEmbedProxy: Boolean = true): no.ndla.learningpathapi.LearningPath = {
      no.ndla.learningpathapi.LearningPath(lp.id.get,
        lp.title.map(asApiTitle),
        lp.description.map(asApiDescription),
        createUrlToLearningPath(lp),
        lp.learningsteps.map(ls => asApiLearningStep(ls, lp, callOEmbedProxy)).toList,
        createUrlToLearningSteps(lp),
        lp.coverPhotoUrl,
        lp.duration,
        lp.status.toString,
        lp.verificationStatus.toString,
        lp.lastUpdated,
        lp.tags.map(asApiLearningPathTag),
        asAuthor(authClient.getUserName(lp.owner)))
    }


    def asApiLearningpathSummary(learningpath: LearningPath): no.ndla.learningpathapi.LearningPathSummary = {
      no.ndla.learningpathapi.LearningPathSummary(learningpath.id.get,
        learningpath.title.map(asApiTitle),
        learningpath.description.map(asApiDescription),
        createUrlToLearningPath(learningpath),
        learningpath.coverPhotoUrl,
        learningpath.duration,
        learningpath.status.toString,
        learningpath.lastUpdated,
        asAuthor(authClient.getUserName(learningpath.owner)))
    }

    def asApiLearningPathSummary(learningPath: no.ndla.learningpathapi.LearningPath): no.ndla.learningpathapi.LearningPathSummary = {
      LearningPathSummary(learningPath.id,
        learningPath.title,
        learningPath.description,
        createUrlToLearningPath(learningPath),
        learningPath.coverPhotoUrl,
        learningPath.duration,
        learningPath.status,
        learningPath.lastUpdated,
        learningPath.author)
    }

    def asApiLearningStep(ls: LearningStep, lp: LearningPath, callOembedProxy: Boolean = true): learningpathapi.LearningStep = {
      no.ndla.learningpathapi.LearningStep(
        ls.id.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.description.map(asApiDescription),
        ls.embedUrl.map(e => asApiEmbedContent(e, callOembedProxy)),
        ls.`type`.toString,
        ls.license, createUrlToLearningStep(ls, lp))
    }

    def asApiTitle(title: no.ndla.learningpathapi.model.Title): no.ndla.learningpathapi.Title = {
      no.ndla.learningpathapi.Title(title.title, title.language)
    }

    def asApiDescription(description: no.ndla.learningpathapi.model.Description): no.ndla.learningpathapi.Description = {
      no.ndla.learningpathapi.Description(description.description, description.language)
    }

    def asApiEmbedContent(embedUrl: no.ndla.learningpathapi.model.EmbedUrl, callOembedProxy: Boolean): no.ndla.learningpathapi.EmbedContent = {
      no.ndla.learningpathapi.EmbedContent(
        embedUrl.url,
        callOembedProxy match {
          case true => oEmbedClient.getHtmlEmbedCodeForUrl(embedUrl.url)
          case false => ""
        },
        embedUrl.language)
    }

    def createUrlToLearningStep(ls: LearningStep, lp: LearningPath): String = {
      s"${createUrlToLearningSteps(lp)}/${ls.id.get}"
    }

    def createUrlToLearningSteps(lp: LearningPath): String = {
      s"${createUrlToLearningPath(lp)}/learningsteps"
    }

    def createUrlToLearningPath(lp: LearningPath): String = {
      lp.isPrivate match {
        case true => s"${ApplicationUrl.get}private/${lp.id.get}"
        case false => s"${ApplicationUrl.get}${lp.id.get}"
      }
    }

    def createUrlToLearningPath(lp: no.ndla.learningpathapi.LearningPath): String = {
      lp.isPrivate match {
        case true => s"${ApplicationUrl.get}private/${lp.id}"
        case false => s"${ApplicationUrl.get}${lp.id}"
      }
    }
  }
}
