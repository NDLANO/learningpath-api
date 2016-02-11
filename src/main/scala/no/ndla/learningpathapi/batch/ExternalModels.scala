package no.ndla.learningpathapi.batch

import java.util.Date

case class Node(nid:Long, tnid:Long, language:String, title:String, packageId:Long) {
  def isTranslation:Boolean = {
    tnid != nid && tnid != 0
  }
}
case class Package(packageId:Long, lastUpdated:Date, packageAuthor:Long, packageTitle:String, durationHours:Int, durationMinutes:Int, language:String, nodeId:Long)
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
              case h if h == "red.ndla.no" => Some(s"http://ndla.no${parsedUri.path}/oembed")
              case h if h == "www.youtube.com" => {
                parsedUri.query.param("v") match {
                  case Some(videoId) => Some(s"https://www.youtube.com/embed/$videoId")
                  case None => Some(url)
                }
              }
              case default => Some(url)
            }
          }
        }
      }
    }
  }
}