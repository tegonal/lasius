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
import core.DBSession

import javax.inject.Inject
import models.BaseFormat._
import models.UserId.UserReference
import models._
import org.joda.time.{DateTime, LocalDateTime}
import play.api.Logging
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.BSONObjectID
import reactivemongo.api.bson.collection.BSONCollection
import repositories.MongoDBCommandSet._

import scala.concurrent._

@ImplementedBy(classOf[BookingHistoryMongoRepository])
trait BookingHistoryRepository
    extends BaseRepository[BookingV2, BookingId]
    with PersistentUserViewRepository[BookingV2, BookingId] {
  def findByUserAndRange(orgId: OrganisationId,
                         userReference: UserReference,
                         from: LocalDateTime,
                         to: LocalDateTime,
                         limit: Option[Int],
                         skip: Option[Int])(implicit
      dbSession: DBSession): Future[Iterable[BookingV2]]

  def findByOrganisationAndRange(orgId: OrganisationId,
                                 from: LocalDateTime,
                                 to: LocalDateTime,
                                 limit: Option[Int],
                                 skip: Option[Int])(implicit
      dbSession: DBSession): Future[Iterable[BookingV2]]

  def findByProjectAndRange(projectId: ProjectId,
                            from: LocalDateTime,
                            to: LocalDateTime,
                            limit: Option[Int],
                            skip: Option[Int])(implicit
      dbSession: DBSession): Future[Iterable[BookingV2]]

  def updateBooking(newBooking: BookingV2)(implicit
      format: Format[BookingV2],
      dbSession: DBSession): Future[Boolean]
}

class BookingHistoryMongoRepository @Inject() ()(
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepository[BookingV2, BookingId]()
    with BookingHistoryRepository
    with MongoPeristentUserViewRepository[BookingV2, BookingId]
    with Logging {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("BookingHistory", failoverStrategy)

  override def findByUserAndRange(orgId: OrganisationId,
                                  userReference: UserReference,
                                  from: LocalDateTime,
                                  to: LocalDateTime,
                                  limit: Option[Int],
                                  skip: Option[Int])(implicit
      dbSession: DBSession): Future[Iterable[BookingV2]] =
    findByRange(Some(userReference), Some(orgId), None, from, to, limit, skip)

  override def findByOrganisationAndRange(orgId: OrganisationId,
                                          from: LocalDateTime,
                                          to: LocalDateTime,
                                          limit: Option[Int],
                                          skip: Option[Int])(implicit
      dbSession: DBSession): Future[Iterable[BookingV2]] =
    findByRange(None, Some(orgId), None, from, to, limit, skip)

  override def findByProjectAndRange(projectId: ProjectId,
                                     from: LocalDateTime,
                                     to: LocalDateTime,
                                     limit: Option[Int],
                                     skip: Option[Int])(implicit
      dbSession: DBSession): Future[Iterable[BookingV2]] =
    findByRange(None, None, Some(projectId), from, to, limit, skip)

  private def findByRange(userReference: Option[UserReference],
                          orgId: Option[OrganisationId],
                          projectId: Option[ProjectId],
                          from: LocalDateTime,
                          to: LocalDateTime,
                          limit: Option[Int],
                          skip: Option[Int])(implicit
      dbSession: DBSession): Future[Iterable[BookingV2]] = {

    val conditions = Seq[Option[(String, JsValueWrapper)]](
      Some("start.dateTime" -> Json.obj(LowerOrEqualsThan -> to)),
      Some("end.dateTime"   -> Json.obj(GreaterOrEqualsThan -> from)),
      userReference.map(ref => "userReference.id" -> ref.id),
      orgId.map(ref => "organisationReference.id" -> ref),
      projectId.map(ref => "projectReference.id" -> ref)
    )

    val sel = Json.obj(conditions.flatten: _*)

    val sortConditions = Seq[Option[(String, JsValueWrapper)]](
      Some("start.dateTime" -> 1),
      userReference.map(ref => "userReference.id" -> 1),
      orgId.map(ref => "organisationReference.id" -> 1),
      projectId.map(ref => "projectReference.id" -> 1),
      Some("_id" -> 1)
    )
    val sort = Json.obj(sortConditions.flatten: _*)

    logger.debug(s"findByUserAndRange:$sel")
    find(sel = sel,
         sort = sort,
         limit = limit.getOrElse(-1),
         skip = skip.getOrElse(0))
      .map(_.map(_._1))
  }

  override def upsert(t: BookingV2)(implicit
      writer: Writes[BookingId],
      dbSession: DBSession): Future[Unit] = {
    logger.debug(s"insertBooking[$t]")
    super.upsert(t)
  }

  override def updateBooking(newBooking: BookingV2)(implicit
      format: Format[BookingV2],
      dbSession: DBSession): Future[Boolean] = {
    val bookingJson = format.writes(newBooking).as[JsObject]
    logger.debug(s"updateBooking[$newBooking], json:$bookingJson")

    update(Json.obj("id" -> newBooking.id), bookingJson, upsert = false)
  }
}
