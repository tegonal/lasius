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
import models.BaseFormat._
import models.UserId.UserReference
import models._
import org.joda.time.{Duration, LocalDate}
import play.api.Logging
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.play.json.compat._
import repositories.MongoDBCommandSet._

import javax.inject.Inject
import scala.concurrent._

trait BookingStatisticRepository[M <: models.OperatorEntity[I, M],
                                 I <: com.tegonal.play.json.TypedId.BaseId[_]]
    extends BaseRepositoryWithOrgRef[M, I]
    with PersistentUserViewRepository[M, I] {

  def findAggregatedByUserAndRange(userReference: UserReference,
                                   orgId: OrganisationId,
                                   from: LocalDate,
                                   to: LocalDate,
                                   aggregationProperty: String,
                                   granularity: Granularity)(implicit
      dbSession: DBSession): Future[List[BookingStats]]

  def findAggregatedByOrganisationAndRange(orgId: OrganisationId,
                                           from: LocalDate,
                                           to: LocalDate,
                                           aggregationProperty: String,
                                           granularity: Granularity)(implicit
      dbSession: DBSession): Future[List[BookingStats]]

  def findAggregatedByProjectAndRange(projectId: ProjectId,
                                      from: LocalDate,
                                      to: LocalDate,
                                      aggregationProperty: String,
                                      granularity: Granularity)(implicit
      dbSession: DBSession): Future[List[BookingStats]]

  def add(model: M)(implicit
      writes: Writes[I],
      dbSession: DBSession): Future[Boolean]

  def subtract(model: M)(implicit
      writes: Writes[I],
      dbSession: DBSession): Future[Boolean]
}

@ImplementedBy(classOf[BookingByProjectMongoRepository])
trait BookingByProjectRepository
    extends BookingStatisticRepository[BookingByProject, BookingByProjectId] {}

@ImplementedBy(classOf[BookingByTagMongoRepository])
trait BookingByTagRepository
    extends BookingStatisticRepository[BookingByTag, BookingByTagId] {}

@ImplementedBy(classOf[BookingByTypeMongoRepository])
trait BookingByTypeRepository
    extends BookingStatisticRepository[BookingByType, BookingByTypeId] {}

abstract class BookingStatisticMongoRepository[
    M <: models.OperatorEntity[I, M],
    I <: com.tegonal.play.json.TypedId.BaseId[_]](implicit
    format: play.api.libs.json.Format[M])
    extends BaseReactiveMongoRepositoryWithOrgRef[M, I]
    with BookingStatisticRepository[M, I]
    with MongoPeristentUserViewRepository[M, I]
    with Logging {

  override def findAggregatedByUserAndRange(userReference: UserReference,
                                            orgId: OrganisationId,
                                            from: LocalDate,
                                            to: LocalDate,
                                            aggregationProperty: String,
                                            granularity: Granularity)(implicit
      dbSession: DBSession): Future[List[BookingStats]] =
    findAggregatedByRange(Some(userReference),
                          Some(orgId),
                          None,
                          from,
                          to,
                          aggregationProperty,
                          granularity)

  override def findAggregatedByOrganisationAndRange(orgId: OrganisationId,
                                                    from: LocalDate,
                                                    to: LocalDate,
                                                    aggregationProperty: String,
                                                    granularity: Granularity)(
      implicit dbSession: DBSession): Future[List[BookingStats]] =
    findAggregatedByRange(None,
                          Some(orgId),
                          None,
                          from,
                          to,
                          aggregationProperty,
                          granularity)

  def findAggregatedByProjectAndRange(projectId: ProjectId,
                                      from: LocalDate,
                                      to: LocalDate,
                                      aggregationProperty: String,
                                      granularity: Granularity)(implicit
      dbSession: DBSession): Future[List[BookingStats]] =
    findAggregatedByRange(None,
                          None,
                          Some(projectId),
                          from,
                          to,
                          aggregationProperty,
                          granularity)

  private def findAggregatedByRange(userReference: Option[UserReference],
                                    orgId: Option[OrganisationId],
                                    projectId: Option[ProjectId],
                                    from: LocalDate,
                                    to: LocalDate,
                                    aggregationProperty: String,
                                    granularity: Granularity)(implicit
      dbSession: DBSession): Future[List[BookingStats]] = {

    val collection = coll
    import collection.AggregationFramework._

    val conditions = Seq[Option[(String, JsValueWrapper)]](
      Some(
        And -> Json.arr(
          Json.obj("day" -> Json.obj(GreaterOrEqualsThan -> from)),
          Json.obj("day" -> Json.obj(LowerOrEqualsThan -> to)))),
      userReference.map(ref => "userReference.id" -> ref.id),
      orgId.map(id => "organisationReference.id" -> id),
      projectId.map(id => "projectReference.id" -> id)
    )

    val sel = Json.obj(conditions.flatten: _*)

    val (additionalGroupByFunctions, sort) = granularity match {
      case All  => (Seq(), Seq(Descending("total")))
      case Year => (Seq("year" -> "$year"), Seq(Ascending("_id.year")))
      case Month =>
        (Seq("year" -> "$year", "month" -> "$month"),
         Seq(Ascending("_id.year"), Ascending("_id.month")))
      case Week =>
        (Seq("year" -> "$year", "week" -> "$week"),
         Seq(Ascending("_id.year"), Ascending("_id.week")))
      case Day =>
        (Seq("year" -> "$year", "month" -> "$month", "day" -> "$dayOfMonth"),
         Seq(Ascending("_id.year"),
             Ascending("_id.month"),
             Ascending("_id.day")))
    }
    val additionGroupBys: Seq[(String, JsValueWrapper)] =
      additionalGroupByFunctions.map { case (field, function) =>
        field -> Json.obj(s"$function" -> Json.obj("$toDate" -> "$day"))
      }
    val groupBys: Seq[(String, JsValueWrapper)] =
      additionGroupBys :+ ("label" -> JsString(s"$$$aggregationProperty"))

    val pipeline = List(
      Match(sel),
      Group(Json.obj(groupBys: _*))("total" -> SumField("duration")),
      Group(
        Json.obj("year"  -> "$_id.year",
                 "month" -> "$_id.month",
                 "week"  -> "$_id.week",
                 "day"   -> "$_id.day"))(
        "values" -> Push(
          Json.obj("label" -> "$_id.label", "duration" -> "$total"))),
      Sort(sort: _*),
      Project(
        Json.obj("_id" -> "$_id", "category" -> "$_id", "values" -> "$values"))
    )

    logger.debug(s"findAggregatedByRange:$this:$sel:$pipeline")

    collection
      .aggregatorContext[BookingStats](pipeline = pipeline)
      .prepared
      .cursor
      .collect[List]()
  }

  override def add(model: M)(implicit
      writes: Writes[I],
      dbSession: DBSession): Future[Boolean] = {
    val sel = getUniqueConstraint(model)
    logger.debug(s"add [$sel]:$model")
    update(sel, Json.obj(Inc -> Json.obj("duration" -> model.duration)))
  }

  override def subtract(model: M)(implicit
      writes: Writes[I],
      dbSession: DBSession): Future[Boolean] = {
    val sel = getUniqueConstraint(model)
    logger.debug(s"subtract [$sel]:$model")
    update(
      sel,
      Json.obj(
        Inc -> Json.obj("duration" -> Duration.ZERO.minus(model.duration)))
    )
  }

  def getUniqueConstraint(model: M): JsObject
}

class BookingByProjectMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BookingStatisticMongoRepository[BookingByProject,
                                            BookingByProjectId](
    )
    with BookingByProjectRepository {
  def coll(implicit dbSession: DBSession): BSONCollection =
    dbSession.db
      .collection[BSONCollection]("BookingByProject", failoverStrategy)

  override def getUniqueConstraint(model: BookingByProject): JsObject = {
    Json.obj("userReference"         -> model.userReference,
             "organisationReference" -> model.organisationReference,
             "day"                   -> model.day,
             "projectReference"      -> model.projectReference)
  }
}

class BookingByTagMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BookingStatisticMongoRepository[BookingByTag, BookingByTagId]
    with BookingByTagRepository {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("BookingByTag", failoverStrategy)

  override def getUniqueConstraint(model: BookingByTag): JsObject = {
    Json.obj("userReference"         -> model.userReference,
             "organisationReference" -> model.organisationReference,
             "day"                   -> model.day,
             "tagId"                 -> model.tagId)
  }
}

class BookingByTypeMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BookingStatisticMongoRepository[BookingByType, BookingByTypeId]
    with BookingByTypeRepository {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("BookingByType", failoverStrategy)

  override def getUniqueConstraint(model: BookingByType): JsObject = {
    Json.obj("userReference"         -> model.userReference,
             "organisationReference" -> model.organisationReference,
             "day"                   -> model.day,
             "bookingType"           -> model.bookingType)
  }
}
