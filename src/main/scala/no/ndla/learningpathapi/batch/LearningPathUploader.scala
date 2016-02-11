package no.ndla.learningpathapi.batch

import no.ndla.learningpathapi.{LearningpathApiProperties, PropertiesLoader}
import no.ndla.learningpathapi.batch.integration.{Tags, CMData, PackageData}
import no.ndla.learningpathapi.integration.AmazonIntegration
import no.ndla.learningpathapi.model._

object LearningPathUploader {

  PropertiesLoader.load()

  val CMPassword = scala.util.Properties.envOrNone("CM_PASSWORD")
  val CMUser = scala.util.Properties.envOrNone("CM_USER")
  val CMHost = scala.util.Properties.envOrNone("CM_HOST")
  val CMPort = scala.util.Properties.envOrNone("CM_PORT")
  val CMDatabase = scala.util.Properties.envOrNone("CM_DATABASE")

  val PackagePassword = scala.util.Properties.envOrNone("PACKAGE_PASSWORD")
  val PackageUser = scala.util.Properties.envOrNone("PACKAGE_USER")
  val PackageHost = scala.util.Properties.envOrNone("PACKAGE_HOST")
  val PackagePort = scala.util.Properties.envOrNone("PACKAGE_PORT")
  val PackageDatabase = scala.util.Properties.envOrNone("PACKAGE_DATABASE")

  val cmData = new CMData(CMHost, CMPort, CMDatabase, CMUser, CMPassword)
  val packageData = new PackageData(PackageHost, PackagePort, PackageDatabase, PackageUser, PackagePassword)
  val learningpathData = AmazonIntegration.getLearningpathData()

  def main(args: Array[String]) {
    val nodes: List[Node] = cmData.allLearningPaths()
    nodes.filterNot(_.isTranslation).foreach(node => {
      importNode(packageData.packageFor(node), List())

      // TODO: Legg til støtte for oversettelser
      // importNode(PackageData.packageFor(node), getTranslations(node, nodes))
    })
  }

  def importNode(optPakke: Option[Package], optTranslations: List[Option[Package]]) = {
    optPakke.foreach(pakke => {
        val learningSteps = packageData.stepsForPackage(pakke).map(asLearningStep)
        val learningPath = asLearningPath(pakke, learningSteps)
        learningpathData.insert(learningPath)
    })
  }

  def asLearningPath(pakke: Package, learningSteps: List[LearningStep]) = {
    val titles = List(Title(pakke.packageTitle, Some(pakke.language)))
    val coverPhotoUrl = None
    val duration = (pakke.durationHours * 60) + pakke.durationMinutes
    val lastUpdated = pakke.lastUpdated
    val tags = Tags.forNodeId(pakke.nodeId)
    val owner = "e646b7f6-60ce-4364-9e77-2a88754b95db" // TODO: Hvordan sette owner? Er nå satt til kes
    val descriptions = learningSteps.find(_.`type` == "1").map(_.description).getOrElse(List())

    no.ndla.learningpathapi.model.LearningPath(
      None,
      titles,
      descriptions,
      coverPhotoUrl,
      duration,
      LearningpathApiProperties.Published,
      LearningpathApiProperties.CreatedByNDLA,
      lastUpdated,
      tags,
      owner,
      learningSteps.filterNot(_.`type` == "1"))
  }

  def asLearningStep(step: Step): LearningStep = {
    val seqNo = step.pos - 1
    val stepType = s"${step.stepType}"
    val title = List(Title(step.title, Some(step.language)))
    val description = step.description match {
      case None => List()
      case Some(desc) => {
        desc.isEmpty match {
          case true => List()
          case false => List(Description(tidyUpDescription(desc), Some(step.language)))
        }
      }
    }

    val embedUrl = step.embedUrlToNdlaNo match {
      case None => List()
      case Some(url) => List(EmbedUrl(url, Some(step.language)))
    }

    LearningStep(None, None, seqNo, title, description, embedUrl, stepType, None)
  }

  def tidyUpDescription(description: String): String = {
    Option(description) match {
      case None => ""
      case Some(desc) => {
        desc.replaceAll("(\\r|\\n|\\t)", "")
          .replaceAll("&aring;", "å")
          .replaceAll("&aelig;", "æ")
          .replaceAll("&oslash;", "ø")
          .replaceAll("&AElig;", "Æ")
          .replaceAll("&Oslash;", "Ø")
          .replaceAll("&Aring;", "Å")
          .replaceAll("&nbsp;", "")
          .replaceAll("<(.*?)>", " ").trim
      }
    }
  }

  def getTranslations(node: Node, nodes:List[Node]): List[Option[Package]] = {
    nodes.filter(candidate => candidate.tnid == node.nid && candidate.nid != node.nid).map(node => packageData.packageFor(node))
  }

  def debug(learningPath: LearningPath) = {
    println(s"${learningPath.title.head.title}")
    learningPath.learningsteps.foreach(step => {
      println(s"    (${step.seqNo}) - ${step.title.head.title} - ${step.embedUrl.headOption.getOrElse(EmbedUrl("NONE",None)).url}")
    })
  }
}

