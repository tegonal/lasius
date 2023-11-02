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
import models.UserId.UserReference
import models._
import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api.bson.collection.BSONCollection

import javax.inject.Inject
import scala.concurrent._

@ImplementedBy(classOf[InvitationMongoRepository])
trait InvitationRepository
    extends BaseRepository[Invitation, InvitationId]
    with Validation {
  def updateInvitationStatus(invitationId: InvitationId,
                             status: InvitationOutcomeStatus)(implicit
      subject: Subject[_],
      dbSession: DBSession): Future[Boolean]

  def updateOrganisationKey(organisationId: OrganisationId, newKey: String)(
      implicit dbSession: DBSession): Future[Boolean]

  def updateProjectKey(projectId: ProjectId, newKey: String)(implicit
      dbSession: DBSession): Future[Boolean]
}

class InvitationMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[Invitation, InvitationId]
    with InvitationRepository {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("Invitation")

  override def updateInvitationStatus(invitationId: InvitationId,
                                      status: InvitationOutcomeStatus)(implicit
      subject: Subject[_],
      dbSession: DBSession): Future[Boolean] = {
    val sel = Json.obj("id" -> invitationId)
    update(
      sel,
      Json.obj(
        MongoDBCommandSet.Set -> Json.obj(
          "outcome" -> Some(
            InvitationOutcome(userReference = subject.userReference,
                              dateTime = DateTime.now(),
                              status = status)))),
      upsert = false
    )
  }

  override def updateOrganisationKey(organisationId: OrganisationId,
                                     newKey: String)(implicit
      dbSession: DBSession): Future[Boolean] = {
    for {
      result1 <- update(Json.obj("organisationReference.id" -> organisationId),
                        Json.obj(
                          MongoDBCommandSet.Set -> Json.obj(
                            "organisationReference.key" -> newKey)),
                        upsert = false)
      result2 <- update(
        Json.obj("sharedByOrganisationReference.id" -> organisationId),
        Json.obj(
          MongoDBCommandSet.Set -> Json.obj(
            "sharedByOrganisationReference.key" -> newKey)),
        upsert = false
      )
    } yield result1 && result2
  }

  override def updateProjectKey(projectId: ProjectId, newKey: String)(implicit
      dbSession: DBSession): Future[Boolean] = {
    val sel =
      Json.obj("projectReference.id" -> projectId)
    update(
      sel,
      Json.obj(
        MongoDBCommandSet.Set -> Json.obj("projectReference.key" -> newKey)),
      upsert = false)
  }
}
