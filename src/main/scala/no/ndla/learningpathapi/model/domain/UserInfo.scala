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
  def isPublisher: Boolean = roles.contains(LearningPathRole.PUBLISH)
  def isWriter: Boolean = roles.contains(LearningPathRole.WRITE)
  def canWriteDuringWriteRestriction: Boolean = isAdmin || isPublisher || isWriter
  def canPublish: Boolean = isAdmin || isPublisher
  def isNdla: Boolean = roles.nonEmpty
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
