/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package repositories

import com.google.inject.ImplementedBy
import core.{DBSession, Validation}
import models._
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.api.bson.collection.BSONCollection
import scalaoauth2.provider.AuthInfo

import java.util.Base64
import javax.inject.Inject
import scala.concurrent._

@ImplementedBy(classOf[OAuthAccessTokenMongoRepository])
trait OAuthAccessTokenRepository
    extends BaseRepository[OAuthAccessToken, OAuthAccessTokenId]
    with DropAllSupport[OAuthAccessToken, OAuthAccessTokenId] {
  def create(authInfo: AuthInfo[OAuthUser])(implicit
      dbSession: DBSession): Future[OAuthAccessToken]

  def findByAuthInfo(authInfo: AuthInfo[OAuthUser])(implicit
      dbSession: DBSession): Future[Option[OAuthAccessToken]]

  def findByRefreshToken(refreshToken: String)(implicit
      dbSession: DBSession): Future[Option[OAuthAccessToken]]

  def findByAccessToken(accessToken: String)(implicit
      dbSession: DBSession): Future[Option[OAuthAccessToken]]

  def refreshToken(authInfo: AuthInfo[OAuthUser], refreshToken: String)(implicit
      dbSession: DBSession): Future[OAuthAccessToken]
}

class OAuthAccessTokenMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[OAuthAccessToken, OAuthAccessTokenId]
    with OAuthAccessTokenRepository
    with MongoDropAllSupport[OAuthAccessToken, OAuthAccessTokenId]
    with Validation {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("OAuthAccessToken")

  private def generateToken(): String = {
    val key = java.util.UUID.randomUUID.toString
    new String(Base64.getEncoder.encode(key.getBytes()))
  }

  override def create(authInfo: AuthInfo[OAuthUser])(implicit
      dbSession: DBSession): Future[OAuthAccessToken] = {
    val accessTokenExpiresIn = 60 * 60 // 1 hour
    for {
      _ <- findByAuthInfo(authInfo).someToFailed(
        s"Duplicate accessToken for user ${authInfo.user.id.value} and client ${authInfo.clientId}")
      newToken <- Future.successful(
        OAuthAccessToken(
          id = OAuthAccessTokenId(),
          accessToken = generateToken(),
          refreshToken = Some(generateToken()),
          userId = authInfo.user.id,
          scope = authInfo.scope,
          expiresIn = accessTokenExpiresIn,
          createdAt = DateTime.now(),
          clientId = authInfo.clientId
        ))
      _ <- upsert(newToken)
    } yield newToken
  }

  override def findByAuthInfo(authInfo: AuthInfo[OAuthUser])(implicit
      dbSession: DBSession): Future[Option[OAuthAccessToken]] = {
    val sel =
      Json.obj("userId" -> authInfo.user.id, "clientId" -> authInfo.clientId)
    findFirst(sel).map(_.map(_._1))
  }

  override def findByRefreshToken(refreshToken: String)(implicit
      dbSession: DBSession): Future[Option[OAuthAccessToken]] = {
    val sel =
      Json.obj("refreshToken" -> refreshToken)
    findFirst(sel).map(_.map(_._1))
  }

  override def findByAccessToken(accessToken: String)(implicit
      dbSession: DBSession): Future[Option[OAuthAccessToken]] = {
    val sel =
      Json.obj("accessToken" -> accessToken)
    findFirst(sel).map(_.map(_._1))
  }

  override def refreshToken(authInfo: AuthInfo[OAuthUser],
                            refreshToken: String)(implicit
      dbSession: DBSession): Future[OAuthAccessToken] = {
    for {
      existingToken <- findByAuthInfo(authInfo).noneToFailed(
        s"Could not find existing accessToken for user ${authInfo.user.id.value} and client ${authInfo.clientId}")
      _ <- failIfNot(
        existingToken.refreshToken.contains(refreshToken),
        s"Refresh token of user ${authInfo.user.id.value} and client ${authInfo.clientId} doesn't match")
      _        <- remove(Json.obj("id" -> existingToken.id))
      newToken <- create(authInfo)
    } yield newToken
  }
}
