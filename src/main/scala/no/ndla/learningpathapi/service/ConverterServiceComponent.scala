package no.ndla.learningpathapi.service

import no.ndla.learningpathapi
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.integration.{OEmbedClientComponent, AuthClientComponent}
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain.{NdlaUserName, EmbedUrl}
import no.ndla.learningpathapi.model._
import no.ndla.network.ApplicationUrl

trait ConverterServiceComponent {
  this: AuthClientComponent with OEmbedClientComponent =>
  val converterService: ConverterService

  class ConverterService {
    def asEmbedUrl(embedContent: EmbedContent): EmbedUrl = {
      domain.EmbedUrl(embedContent.url, embedContent.language)
    }

    def asDescription(description: Description): domain.Description = {
      domain.Description(description.description, description.language)
    }

    def asTitle(title: Title): domain.Title = {
      domain.Title(title.title, title.language)
    }


    def asLearningPathTag(tag: LearningPathTag): domain.LearningPathTag = {
      domain.LearningPathTag(tag.tag, tag.language)
    }

    def asApiLearningPathTag(tag: domain.LearningPathTag): LearningPathTag = {
      LearningPathTag(tag.tag, tag.language)
    }

    def asAuthor(user: NdlaUserName): Author = {
      val names = Array(user.first_name, user.middle_name, user.last_name).filter(_.isDefined).map(_.get)
      Author("Forfatter", names.mkString(" "))
    }

    def asApiLearningpath(lp: domain.LearningPath, user: Option[String]): LearningPath = {
      api.LearningPath(lp.id.get,
        lp.revision.get,
        lp.isBasedOn,
        lp.title.map(asApiTitle),
        lp.description.map(asApiDescription),
        createUrlToLearningPath(lp),
        lp.learningsteps.map(ls => asApiLearningStepSummary(ls, lp)).toList.sortBy(_.seqNo),
        createUrlToLearningSteps(lp),
        lp.coverPhotoUrl,
        lp.duration,
        lp.status.toString,
        lp.verificationStatus.toString,
        lp.lastUpdated,
        lp.tags.map(asApiLearningPathTag),
        asAuthor(authClient.getUserName(lp.owner)),
        lp.canEdit(user))
    }


    def asApiLearningpathSummary(learningpath: domain.LearningPath): LearningPathSummary = {
      api.LearningPathSummary(learningpath.id.get,
        learningpath.title.map(asApiTitle),
        learningpath.description.map(asApiDescription),
        createUrlToLearningPath(learningpath),
        learningpath.coverPhotoUrl,
        learningpath.duration,
        learningpath.status.toString,
        learningpath.lastUpdated,
        asAuthor(authClient.getUserName(learningpath.owner)))
    }

    def asApiLearningPathSummary(learningPath: LearningPath): LearningPathSummary = {
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

    def asApiLearningStep(ls: domain.LearningStep, lp: domain.LearningPath, user: Option[String]): LearningStep = {
      LearningStep(
        ls.id.get,
        ls.revision.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.description.map(asApiDescription),
        ls.embedUrl.map(e => asApiEmbedContent(e)),
        ls.`type`.toString,
        ls.license, createUrlToLearningStep(ls, lp),
        lp.canEdit(user))
    }

    def asApiLearningStepSummary(ls: domain.LearningStep, lp: domain.LearningPath): LearningStepSummary = {
      LearningStepSummary(
        ls.id.get,
        ls.seqNo,
        ls.title.map(asApiTitle),
        ls.`type`.toString,
        createUrlToLearningStep(ls, lp)
      )
    }

    def asApiTitle(title: domain.Title): Title = {
      api.Title(title.title, title.language)
    }

    def asApiDescription(description: domain.Description): Description = {
      api.Description(description.description, description.language)
    }

    def asApiEmbedContent(embedUrl: EmbedUrl): EmbedContent = {
      api.EmbedContent(
        embedUrl.url,
        oEmbedClient.getHtmlEmbedCodeForUrl(embedUrl.url),
        embedUrl.language)
    }

    def createUrlToLearningStep(ls: domain.LearningStep, lp: domain.LearningPath): String = {
      s"${createUrlToLearningSteps(lp)}/${ls.id.get}"
    }

    def createUrlToLearningSteps(lp: domain.LearningPath): String = {
      s"${createUrlToLearningPath(lp)}/learningsteps"
    }

    def createUrlToLearningPath(lp: domain.LearningPath): String = {
      s"${ApplicationUrl.get}${lp.id.get}"
    }

    def createUrlToLearningPath(lp: LearningPath): String = {
      s"${ApplicationUrl.get}${lp.id}"
    }
  }
}
