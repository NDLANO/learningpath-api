package no.ndla.learningpathapi.batch

import java.util.Date

case class Node(nid:Long, tnid:Long, language:String, title:String, packageId:Long, imageNid: Option[Int], description: String) {
  def isTranslation:Boolean = {
    tnid != nid && tnid != 0
  }
}
case class Package(packageId:Long, lastUpdated:Date, packageAuthor:Long, packageTitle:String, durationHours:Int, durationMinutes:Int, language:String, nodeId:Long, description: String)
case class Step(packageId:Long, pageId:Long, pos:Int, title:String, stepType:Long, pageAuthor:Long, embedUrl:Option[String], description:Option[String], language:String) {
  def embedUrlToNdlaNo:Option[String] = {
    embedUrl match {
      case None => None
      case Some(url) => {
        val parsedUri = com.netaporter.uri.Uri.parse(url)
        parsedUri.host match {
          case None => None
          case Some(host) => {
            host match {
              case h if h == "red.ndla.no" => Some(s"http://ndla.no${parsedUri.path}")
              case default => Some(url)
            }
          }
        }
      }
    }
  }
}