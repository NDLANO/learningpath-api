/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import java.util

import com.typesafe.scalalogging.LazyLogging
import javax.servlet.DispatcherType
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener
import scala.jdk.CollectionConverters._

import scala.io.Source

object JettyLauncher extends LazyLogging {

  def startServer(port: Int): Server = {
    logger.info(
      Source
        .fromInputStream(getClass.getResourceAsStream("/log-license.txt"))
        .mkString)

    val startDBMillis = System.currentTimeMillis()
    DBMigrator.migrate(ComponentRegistry.dataSource)
    logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")

    val startMillis = System.currentTimeMillis

    val servletContext = new ServletContextHandler
    servletContext.setContextPath("/")
    servletContext.addEventListener(new ScalatraListener)
    servletContext.addServlet(classOf[DefaultServlet], "/")
    servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    servletContext.addServlet(classOf[ReportServlet], "/monitoring")
    servletContext.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, LearningpathApiProperties.ApplicationName)
    LearningpathApiProperties.Environment match {
      case "local" => None
      case _ =>
        monitoringFilter.setInitParameter(Parameter.CLOUDWATCH_NAMESPACE.getCode,
                                          "NDLA/APP".replace("APP", LearningpathApiProperties.ApplicationName))
    }
    servletContext.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    val server = new Server(port)
    server.setHandler(servletContext)
    server.start()

    val startTime = System.currentTimeMillis - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server
  }

  def main(args: Array[String]): Unit = {
    val envMap = System.getenv()
    envMap.asScala.foreach { case (k, v) => System.setProperty(k, v) }

    val server = startServer(LearningpathApiProperties.ApplicationPort)
    server.join()
  }
}
