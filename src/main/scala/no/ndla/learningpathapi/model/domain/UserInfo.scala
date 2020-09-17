/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.domain
import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.AuthUser

import scala.util.{Failure, Success}

case class UserInfo(userId: String, roles: Set[LearningPathRole.Value]) {
  def isAdmin: Boolean = roles.contains(LearningPathRole.ADMIN)
  def isPublisher: Boolean = roles.contains(LearningPathRole.PUBLISH)
  def isWriter: Boolean = roles.contains(LearningPathRole.WRITE)
  def canWriteDuringWriteRestriction: Boolean = isAdmin || isPublisher || isWriter
  def canPublish: Boolean = isAdmin || isPublisher
  def isNdla: Boolean = roles.nonEmpty
}

object UserInfo extends LazyLogging {
  val PublicReadUser = UserInfo("PublicReadUser", Set.empty)

  def apply(name: String): UserInfo = {
    new UserInfo(
      name,
      AuthUser.getRoles.flatMap(LearningPathRole.valueOf).toSet
    )
  }

  def getUser = AuthUser.get.map(UserInfo.apply)
  def getUserOrPublic: UserInfo = getUser.getOrElse(PublicReadUser)

  def get: Option[UserInfo] = AuthUser.get.orElse(AuthUser.getClientId).map(UserInfo.apply)

  def getWithUserIdOrAdmin = {
    AuthUser.get match {
      case Some(userId) => Success(UserInfo(userId))
      case None =>
        this.get match {
          case Some(user) if user.isAdmin => Success(user)
          case _                          => Failure(AccessDeniedException("You do not have access to the requested resource."))
        }
    }
  }
}
