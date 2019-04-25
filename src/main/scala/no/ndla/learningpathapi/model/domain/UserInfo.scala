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
  def canWriteDuringExams: Boolean = isAdmin || roles.contains(LearningPathRole.WRITE)
}

object UserInfo {
  val PublicReadUser = UserInfo("PublicReadUser", Set.empty)

  def apply(name: String): UserInfo = {
    new UserInfo(
      name,
      AuthUser.getRoles.flatMap(LearningPathRole.valueOf).toSet
    )
  }

  def get: UserInfo = AuthUser.get.map(UserInfo.apply).getOrElse(PublicReadUser)
}
