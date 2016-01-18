package no.ndla.learningpathapi

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source

object JettyLauncher extends LazyLogging {
  def main(args: Array[String]) {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    LearningpathApiProperties.verify()

    val startMillis = System.currentTimeMillis
    val port = LearningpathApiProperties.ApplicationPort

    val servletContext = new ServletContextHandler
    servletContext.setContextPath("/")
    servletContext.setVirtualHosts(LearningpathApiProperties.Domains)
    servletContext.addEventListener(new ScalatraListener)
    servletContext.addServlet(classOf[DefaultServlet], "/")
    servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    val server = new Server(port)
    server.setHandler(servletContext)
    server.start()

    val startTime = System.currentTimeMillis - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server.join()
  }
}
