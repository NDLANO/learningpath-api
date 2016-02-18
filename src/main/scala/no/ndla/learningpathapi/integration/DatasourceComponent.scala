package no.ndla.learningpathapi.integration

import javax.sql.DataSource

trait DatasourceComponent {
  val datasource: DataSource
}
