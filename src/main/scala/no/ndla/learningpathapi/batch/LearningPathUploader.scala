package no.ndla.learningpathapi.batch

import com.typesafe.scalalogging.LazyLogging
import no.ndla.learningpathapi.PropertiesLoader

object LearningPathUploader extends LazyLogging {
  PropertiesLoader.load()

  def main (args: Array[String]){
    if(args.length != 1){
      logger.info("One arguments required: <environment>")
      System.exit(1)
    }

    val env = args(0) match {
      case "prod" | "test" | "staging" =>  args(0)
      case _ => "test"
    }

    BatchComponentRegistry.importService.doImport(env)

  }
}


