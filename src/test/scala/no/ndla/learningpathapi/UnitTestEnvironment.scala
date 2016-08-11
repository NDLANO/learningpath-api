/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import scalikejdbc.DBSession


trait UnitTestEnvironment extends TestEnvironment {
  override def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null):A = {
    work(mock[DBSession])
  }
}
