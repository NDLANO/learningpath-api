/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.integration

import no.ndla.learningpathapi.model.domain.NdlaUserName
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._

import scala.util.{Failure, Success}
import scalaj.http.HttpRequest

class AuthClientTest extends UnitSuite with UnitTestEnvironment {

  val NdlaUsername = NdlaUserName(Some("Fornavn"), Some("Mellomnavn"), Some("Etternavn"))
  override val authClient = new AuthClient

  test("That username is returned when fetching") {
    when(ndlaClient.fetch[NdlaUserName](any[HttpRequest])(any[Manifest[NdlaUserName]])).thenReturn(Success(NdlaUsername))
    authClient.getUserName("abc") should equal(NdlaUsername)
  }

  test("That unknown user us returned if any failure occurs") {
    when(ndlaClient.fetch[NdlaUserName](any[HttpRequest])(any[Manifest[NdlaUserName]])).thenReturn(Failure(new Exception("An error")))
    authClient.getUserName("abc") should equal(authClient.unknownUser)
  }
}
