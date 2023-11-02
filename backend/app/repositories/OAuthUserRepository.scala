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
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.json._
import reactivemongo.api.bson.collection.BSONCollection

import javax.inject.Inject
import scala.concurrent._
// Conversions from BSON to JSON extended syntax
//import reactivemongo.play.json.compat.bson2json._

@ImplementedBy(classOf[OAuthUserMongoRepository])
trait OAuthUserRepository
    extends BaseRepository[OAuthUser, OAuthUserId]
    with DropAllSupport[OAuthUser, OAuthUserId] {
  def authenticate(email: String, password: String)(implicit
      dbSession: DBSession): Future[Option[OAuthUser]]

  def create(registration: OAuthUserRegistration)(implicit
      dbSession: DBSession): Future[OAuthUser]

  def validateCreate(registration: OAuthUserRegistration)(implicit
      dbSession: DBSession
  ): Future[Boolean]

  def changePassword(email: String,
                     passwordChangeRequest: PasswordChangeRequest)(implicit
      dbSession: DBSession): Future[Boolean]
}

class OAuthUserMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[OAuthUser, OAuthUserId]
    with OAuthUserRepository
    with MongoDropAllSupport[OAuthUser, OAuthUserId]
    with Validation {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("OAuthUser")

  def findByEmail(email: String)(implicit
      dbSession: DBSession): Future[Option[OAuthUser]] = {
    val sel = Json.obj("email" -> email)
    findFirst(sel).map(_.map(_._1))
  }

  private def checkPwd(user: OAuthUser, password: String)(implicit
      dbSession: DBSession): Boolean =
    BCrypt.checkpw(password, user.password)

  override def authenticate(email: String, password: String)(implicit
      dbSession: DBSession): Future[Option[OAuthUser]] = {
    findByEmail(email).map {
      case Some(user) if checkPwd(user, password) => Some(user)
      case _                                      => None
    }
  }

  override def create(registration: OAuthUserRegistration)(implicit
      dbSession: DBSession): Future[OAuthUser] = {
    for {
      _ <- validateCreate(registration)
      newUser = OAuthUser(
        id = OAuthUserId(),
        email = registration.email,
        password = BCrypt.hashpw(registration.password, BCrypt.gensalt()),
        firstName = registration.firstName,
        lastName = registration.lastName,
        active = true
      )
      _ <- upsert(newUser)
    } yield newUser
  }

  override def validateCreate(registration: OAuthUserRegistration)(implicit
      dbSession: DBSession): Future[Boolean] = {
    for {
      _            <- validateNonBlankString("email", registration.email)
      existingUser <- findByEmail(registration.email)
      _            <- validate(existingUser.isEmpty, s"user_already_registered")
      _            <- validatePasswordPolicy(registration.password)
    } yield true
  }

  override def changePassword(email: String,
                              passwordChangeRequest: PasswordChangeRequest)(
      implicit dbSession: DBSession): Future[Boolean] = {
    for {
      _ <- validate(
        passwordChangeRequest.password != passwordChangeRequest.newPassword,
        s"It is not allowed to use the old password as new password")
      _ <- validatePasswordPolicy(passwordChangeRequest.newPassword)
      user <- findByEmail(email).noneToFailed(
        s"Could not find user with email $email")
      _ <- validate(checkPwd(user, passwordChangeRequest.password),
                    s"Provided password does not match")
      result <- updateFields(
        userSelection(user.id),
        Seq(
          "password" -> BCrypt.hashpw(passwordChangeRequest.newPassword,
                                      BCrypt.gensalt())))
      updatedUser <- findById(user.id).noneToFailed(
        s"Could not find user with id ${user.id.value}")
      _ <- validate(checkPwd(updatedUser, passwordChangeRequest.newPassword),
                    s"Failed changing password")
    } yield result
  }

  private def userSelection(userId: OAuthUserId) = {
    Json.obj("id" -> userId)
  }
}
