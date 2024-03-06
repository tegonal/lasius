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
import models.BaseFormat._
import models.OrganisationId.OrganisationReference
import models._
import org.joda.time.LocalDate
import play.api.libs.json.Json
import reactivemongo.api.bson.collection.BSONCollection

import javax.inject.Inject
import scala.concurrent._

@ImplementedBy(classOf[PublicHolidayMongoRepository])
trait PublicHolidayRepository
    extends BaseRepositoryWithOrgRef[PublicHoliday, PublicHolidayId]
    with DropAllSupport[PublicHoliday, PublicHolidayId]
    with Validation {
  def findByOrganisationAndYear(organisationReference: OrganisationReference,
                                year: Int)(implicit
      dbSession: DBSession): Future[Seq[PublicHoliday]]

  def create(organisationReference: OrganisationReference,
             createObject: CreatePublicHoliday)(implicit
      subject: Subject,
      dbSession: DBSession): Future[PublicHoliday]

  def update(organisationReference: OrganisationReference,
             id: PublicHolidayId,
             update: UpdatePublicHoliday)(implicit
      subject: Subject,
      dbSession: DBSession): Future[PublicHoliday]
}

class PublicHolidayMongoRepository @Inject() (
    override implicit protected val executionContext: ExecutionContext)
    extends BaseReactiveMongoRepositoryWithOrgRef[PublicHoliday,
                                                  PublicHolidayId]
    with PublicHolidayRepository
    with MongoDropAllSupport[PublicHoliday, PublicHolidayId] {
  override protected[repositories] def coll(implicit
      dbSession: DBSession): BSONCollection =
    dbSession.db.collection[BSONCollection]("PublicHoliday")

  override def findByOrganisationAndYear(
      organisationReference: OrganisationReference,
      year: Int)(implicit dbSession: DBSession): Future[Seq[PublicHoliday]] = {
    find(
      Json.obj("organisationReference.id" -> organisationReference.id,
               "year"                     -> year)).map { result =>
      result.map(_._1).toSeq
    }
  }

  private def findByOrgAndDate(organisationReference: OrganisationReference,
                               date: LocalDate)(implicit
      dbSession: DBSession): Future[Option[PublicHoliday]] = {
    findFirst(
      Json.obj("organisationReference.id" -> organisationReference.id,
               "date"                     -> date)).map { result =>
      result.map(_._1)
    }
  }

  override def create(organisationReference: OrganisationReference,
                      createObject: CreatePublicHoliday)(implicit
      subject: Subject,
      dbSession: DBSession): Future[PublicHoliday] = {
    for {
      existingObject <- findByOrgAndDate(organisationReference,
                                         createObject.date)
      _ <- validate(
        existingObject.isEmpty,
        s"Cannot create duplicate public holidays entry with same date ${createObject.date} in organisation ${organisationReference.key}"
      )
      publicHoliday = PublicHoliday(
        id = PublicHolidayId(),
        date = createObject.date,
        year = createObject.date.getYear,
        organisationReference = organisationReference,
        name = createObject.name
      )
      _ <- upsert(publicHoliday)
    } yield publicHoliday
  }

  override def update(organisationReference: OrganisationReference,
                      id: PublicHolidayId,
                      update: UpdatePublicHoliday)(implicit
      subject: Subject,
      dbSession: DBSession): Future[PublicHoliday] = {
    for {
      result <- updateFields(
        Json.obj("id"                       -> id,
                 "organisationReference.id" -> organisationReference.id),
        Seq(
          "name" -> update.name
        ))
      _ <- validate(
        result,
        s"Updating of public holiday ${id.value} in ${organisationReference.key} failed")
      result <- findByOrganisationAndId(organisationReference, id)
        .noneToFailed(
          s"Failed loading updated public holiday ${id.value} in organisation ${organisationReference.key}")
    } yield result
  }
}
