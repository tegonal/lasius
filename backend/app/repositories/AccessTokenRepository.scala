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
import models.OrganisationId.OrganisationReference
import models.ProjectId.ProjectReference
import models.UserId.UserReference
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import reactivemongo.api.bson.collection.BSONCollection

import javax.inject.Inject
import scala.concurrent._

@ImplementedBy(classOf[AccessTokenMongoRepository])
trait AccessTokenRepository
    extends BaseRepository[AccessToken, AccessTokenId]
    with DropAllSupport[AccessToken, AccessTokenId] {
  def findByUserId(userId: UserId)(implicit
      dbSession: DBSession): Future[List[AccessToken]]

  def resolveAndValidateToken(accessTokenId: AccessTokenId, secret: String)(
      implicit dbSession: DBSession): Future[Option[UserReference]]
}

class AccessTokenMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[AccessToken, AccessTokenId]
    with AccessTokenRepository
    with MongoDropAllSupport[AccessToken, AccessTokenId]
    with Validation {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("AccessToken")

  override def findByUserId(userId: UserId)(implicit
      dbSession: DBSession): Future[List[AccessToken]] = {
    val sel = Json.obj("userReference.id" -> userId)
    find(sel).map(_.map(_._1).toList)
  }

  override def resolveAndValidateToken(accessTokenId: AccessTokenId,
                                       secret: String)(implicit
      dbSession: DBSession): Future[Option[UserReference]] = {
    findById(accessTokenId).map {
      case Some(accessToken) if checkSecret(accessToken, secret) =>
        Some(accessToken.user)
      case _ => None
    }
  }

  private def checkSecret(accessToken: AccessToken,
                          tokenSecret: String): Boolean =
    BCrypt.checkpw(tokenSecret, accessToken.tokenSecretHash)

}
