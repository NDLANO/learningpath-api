package no.ndla.learningpathapi.service

import java.util.Date

import no.ndla.learningpathapi
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model.{LearningStep, LearningPath}
import no.ndla.network.ApplicationUrl


object ModelConverters {
  def asEmbedUrl(url: EmbedUrl): model.EmbedUrl = {
    model.EmbedUrl(url.url, url.language)
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

  def asLearningPath(newLearningPath: NewLearningPath, publishingStatus:String): LearningPath = {
    LearningPath(None,
      newLearningPath.title.map(asTitle),
      newLearningPath.description.map(asDescription),
      newLearningPath.coverPhotoUrl,
      newLearningPath.duration,
      publishingStatus,
      LearningpathApiProperties.External, // TODO: Regler for å sette disse
      new Date(),
      newLearningPath.tags.map(asLearningPathTag), "", List())
  }

  def asApiLearningPathTag(tag: model.LearningPathTag): LearningPathTag = {
    LearningPathTag(tag.tag, tag.language)
  }

  def asApiLearningpath(lp: LearningPath): no.ndla.learningpathapi.LearningPath = {
    no.ndla.learningpathapi.LearningPath(lp.id.get,
      lp.title.map(asApiTitle),
      lp.description.map(asApiDescription),
      createUrlToLearningPath(lp),
      lp.learningsteps.map(ls => asApiLearningStep(ls, lp)).toList,
      createUrlToLearningSteps(lp),
      lp.coverPhotoUrl,
      lp.duration,
      lp.status,
      lp.verificationStatus,
      lp.lastUpdated,
      lp.tags.map(asApiLearningPathTag),
      Author("Forfatter", "Not yet implemented")) // TODO: Hent navn fra auth, basert på owner
  }


  def asApiLearningpathSummary(learningpath: LearningPath): no.ndla.learningpathapi.LearningPathSummary = {
    no.ndla.learningpathapi.LearningPathSummary(learningpath.id.get,
      learningpath.title.map(asApiTitle),
      learningpath.description.map(asApiDescription),
      createUrlToLearningPath(learningpath),
      learningpath.coverPhotoUrl,
      learningpath.duration,
      learningpath.status,
      learningpath.lastUpdated,
      Author("Forfatter", "Not yet implemented")) // TODO: Hent navn fra auth, basert på owner
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

  def asApiLearningStep(ls: LearningStep, lp: LearningPath): learningpathapi.LearningStep = {
    no.ndla.learningpathapi.LearningStep(
      ls.id.get,
      ls.seqNo,
      ls.title.map(asApiTitle),
      ls.description.map(asApiDescription),
      ls.embedUrl.map(asApiEmbedUrl),
      ls.`type`,
      ls.license, createUrlToLearningStep(ls, lp))
  }

  def asApiTitle(title: no.ndla.learningpathapi.model.Title): no.ndla.learningpathapi.Title = {
    no.ndla.learningpathapi.Title(title.title, title.language)
  }

  def asApiDescription(description: no.ndla.learningpathapi.model.Description): no.ndla.learningpathapi.Description = {
    no.ndla.learningpathapi.Description(description.description, description.language)
  }
  
  def asApiEmbedUrl(embedUrl: no.ndla.learningpathapi.model.EmbedUrl): no.ndla.learningpathapi.EmbedUrl = {
    no.ndla.learningpathapi.EmbedUrl(embedUrl.url, embedUrl.language)
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
