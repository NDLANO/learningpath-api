package no.ndla.learningpathapi.batch

import no.ndla.learningpathapi.PropertiesLoader

object LearningPathUploader {
  PropertiesLoader.load()

  def main (args: Array[String]){
    BatchComponentRegistry.importService.doImport()
  }
}


