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

package controllers

import com.typesafe.config.Config
import core.{DBSession, DBSupport}
import models.{OAuthAccessToken, OAuthUser}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{
  OAuthAccessTokenRepository,
  OAuthAuthorizationCodeRepository,
  OAuthUserRepository
}
import scalaoauth2.provider._

import scala.concurrent.{ExecutionContext, Future}

class SimpleInternalDataHandler(
    override val supportTransaction: Boolean,
    override val reactiveMongoApi: ReactiveMongoApi,
    oauthUserRepository: OAuthUserRepository,
    oauthAccessTokenRepository: OAuthAccessTokenRepository,
    oauthAuthorizationCodeRepository: OAuthAuthorizationCodeRepository,
    typesafeConfig: Config
)(implicit ec: ExecutionContext)
    extends DataHandler[OAuthUser]
    with DBSupport {

  private val oauthClientId =
    typesafeConfig.getString("lasius.oauth2_provider.client_id")
  private val oauthClientSecret =
    typesafeConfig.getString("lasius.oauth2_provider.client_secret")

  def validateClient(maybeClientCredential: Option[ClientCredential],
                     request: AuthorizationRequest): Future[Boolean] =
    Future.successful(maybeClientCredential.exists { clientCredential =>
      clientCredential.clientId == oauthClientId && clientCredential.clientSecret
        .contains(oauthClientSecret)
    })

  def findUser(maybeClientCredential: Option[ClientCredential],
               request: AuthorizationRequest): Future[Option[OAuthUser]] =
    request match {
      case p: PasswordRequest =>
        withDBSession() { implicit dbSession =>
          oauthUserRepository.authenticate(p.username, p.password)
        }

      case _ => Future.successful(None)
    }

  def createAccessToken(authInfo: AuthInfo[OAuthUser]): Future[AccessToken] =
    withDBSession() { implicit dbSession =>
      oauthAccessTokenRepository.create(authInfo).map(_.toAccessToken)
    }

  def getStoredAccessToken(
      authInfo: AuthInfo[OAuthUser]): Future[Option[AccessToken]] =
    withDBSession() { implicit dbSession =>
      oauthAccessTokenRepository
        .findByAuthInfo(authInfo)
        .map(_.map(_.toAccessToken))
    }

  def refreshAccessToken(authInfo: AuthInfo[OAuthUser],
                         refreshToken: String): Future[AccessToken] =
    withDBSession() { implicit dbSession =>
      oauthAccessTokenRepository
        .refreshToken(authInfo, refreshToken)
        .map(_.toAccessToken)
    }

  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[OAuthUser]]] = {
    withDBSession() { implicit dbSession =>
      for {
        maybeCode <- oauthAuthorizationCodeRepository.findByCode(code)
        maybeUser <- maybeCode
          .map(oauthCode => oauthUserRepository.findById(oauthCode.userId))
          .getOrElse(Future.successful(None))
      } yield {
        (maybeCode, maybeUser) match {
          case (Some(code), Some(user)) => Some(user.toAuthInfo(code))
          case _                        => None
        }
      }
    }
  }

  def findAuthInfoByRefreshToken(
      refreshToken: String): Future[Option[AuthInfo[OAuthUser]]] =
    withDBSession() { implicit dbSession =>
      findByToken(refreshToken, oauthAccessTokenRepository.findByRefreshToken)
    }

  def findAuthInfoByAccessToken(
      accessToken: AccessToken): Future[Option[AuthInfo[OAuthUser]]] =
    withDBSession() { implicit dbSession =>
      findByToken(accessToken.token,
                  oauthAccessTokenRepository.findByAccessToken)
    }

  private def findByToken(
      token: String,
      tokenResolver: String => Future[Option[OAuthAccessToken]])(implicit
      dbSession: DBSession): Future[Option[AuthInfo[OAuthUser]]] =
    for {
      maybeToken <- tokenResolver(token)
      maybeUser <- maybeToken
        .map(token => oauthUserRepository.findById(token.userId))
        .getOrElse(Future.successful(None))
    } yield {
      (maybeToken, maybeUser) match {
        case (Some(token), Some(user)) => Some(user.toAuthInfo(token))
        case _                         => None
      }
    }

  def deleteAuthCode(code: String): Future[Unit] = {
    withDBSession() { implicit dbSession =>
      oauthAuthorizationCodeRepository
        .removeByCode(code)
        .map(_ => ())
    }
  }

  def findAccessToken(token: String): Future[Option[AccessToken]] =
    withDBSession() { implicit dbSession =>
      oauthAccessTokenRepository
        .findByAccessToken(token)
        .map(_.map(_.toAccessToken))
    }
}
