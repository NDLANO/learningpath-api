package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.Author
import no.ndla.learningpathapi.model.LearningPath


object ModelConverters {

  def asLearningpath(lp: LearningPath): no.ndla.learningpathapi.LearningPath = {
    no.ndla.learningpathapi.LearningPath(lp.id.get,
      lp.title.map(asApiTitle),
      lp.description.map(asApiDescription),
      lp.learningsteps.map(asApiLearningStep),
      "TODO: CREATE LEARNINGSTEPURL",
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
      "CREATE METAURL",
      learningpath.coverPhotoUrl,
      learningpath.duration,
      learningpath.status,
      learningpath.lastUpdated,
      Author("Forfatter", "TODO: Hent fra Auth"))
  }


  def asApiLearningStep(ls: no.ndla.learningpathapi.model.LearningStep): no.ndla.learningpathapi.LearningStep = {
    no.ndla.learningpathapi.LearningStep(ls.id, ls.seqNo, ls.title.map(asApiTitle), ls.embedUrl.map(asApiEmbedUrl(_)), ls.`type`, "TODO: Create META URL")
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

}
