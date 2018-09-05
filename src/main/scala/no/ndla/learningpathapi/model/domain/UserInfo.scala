/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain
import no.ndla.network.AuthUser

case class UserInfo(userId: String, roles: Set[LearningPathRole.Value]) {
  def isAdmin: Boolean = roles.contains(LearningPathRole.ADMIN)
}

object UserInfo {

  def apply(name: String): UserInfo = {
    new UserInfo(
      name,
      AuthUser.getRoles.flatMap(LearningPathRole.valueOf).toSet
    )
  }

  def get: Option[UserInfo] = apply(AuthUser.get)

  def apply(name: Option[String]): Option[UserInfo] = name.map(apply)

}
