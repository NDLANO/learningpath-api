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
    val descriptions = learningSteps.find(_.`type` == "1").map(_.description).getOrElse(List())

    val owner = pakke.nodeId match {
      case 149862 => "6e74cde7-1e83-49c8-8dcd-9bbef458f477" //Christer
      case 156729 => "d6b2bbd0-2dd4-485a-9d9a-af2e7c9d57ad" //RST
      case 156987 => "ddd2ff24-616a-484d-8172-55ddba52cd7a" //KW
      case 149007 => "a62debc3-74a7-43f3-88c9-d35837a41698" //Remi
      case 143822 => "e646b7f6-60ce-4364-9e77-2a88754b95db" // KES
      case default => "e646b7f6-60ce-4364-9e77-2a88754b95db" //KES
    }

    val publishStatus = pakke.nodeId match {
      case 149862 => LearningPathStatus.PRIVATE
      case 156729 => LearningPathStatus.PRIVATE
      case 156987 => LearningPathStatus.PRIVATE
      case 149007 => LearningPathStatus.PRIVATE
      case 143822 => LearningPathStatus.PRIVATE
      case default => LearningPathStatus.PUBLISHED
    }

    no.ndla.learningpathapi.model.LearningPath(
      None,
      titles,
      descriptions,
      coverPhotoUrl,
      duration,
      publishStatus,
      LearningPathVerificationStatus.CREATED_BY_NDLA,
      lastUpdated,
      tags,
      owner,
      learningSteps.filterNot(_.`type` == "1"))
  }

  def asLearningStepType(stepType: String): StepType.Value = {
    stepType match {
      case "2" => StepType.TEXT
      case "3" => StepType.QUIZ
      case "4" => StepType.TASK
      case "5" => StepType.MULTIMEDIA
      case "6" => StepType.SUMMARY
      case "7" => StepType.TEST
      case "8" => StepType.SUMMARY
      case default => StepType.TEXT
    }
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

    LearningStep(None, None, seqNo, title, description, embedUrl, asLearningStepType(stepType), None)
  }

  def tidyUpDescription(description: String): String = {
    Option(description) match {
      case None => ""
      case Some(desc) => {
        desc.replaceAll("(\\r|\\n|\\t)", "")
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

