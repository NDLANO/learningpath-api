package no.ndla.learningpathapi.service

import no.ndla.learningpathapi
import no.ndla.learningpathapi.Author
import no.ndla.learningpathapi.model.{LearningStep, LearningPath}
import no.ndla.network.ApplicationUrl


object ModelConverters {

  def asLearningpath(lp: LearningPath): no.ndla.learningpathapi.LearningPath = {
    no.ndla.learningpathapi.LearningPath(lp.id.get,
      lp.title.map(asApiTitle),
      lp.description.map(asApiDescription),
      lp.learningsteps.map(ls => asApiLearningStep(ls, lp)),
      createUrlToLearningSteps(lp),
      lp.coverPhotoUrl,
      lp.duration,
      lp.status,
      lp.lastUpdated,
      Author("Forfatter", "TODO: Hent fra Auth"))
  }


  def asLearningpathSummary(learningpath: LearningPath): no.ndla.learningpathapi.LearningPathSummary = {
    no.ndla.learningpathapi.LearningPathSummary(learningpath.id.get,
      learningpath.title.map(asApiTitle),
      learningpath.description.map(asApiDescription),
      createUrlToLearningPath(learningpath),
      learningpath.coverPhotoUrl,
      learningpath.duration,
      learningpath.status,
      learningpath.lastUpdated,
      Author("Forfatter", "TODO: Hent fra Auth"))
  }



  def asApiLearningStep(ls: LearningStep, lp: LearningPath): learningpathapi.LearningStep = {
    no.ndla.learningpathapi.LearningStep(ls.id, ls.seqNo, ls.title.map(asApiTitle), ls.embedUrl.map(asApiEmbedUrl(_)), ls.`type`, createUrlToLearningStep(ls, lp))
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
    s"${createUrlToLearningSteps(lp)}/${ls.id}"
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
}
