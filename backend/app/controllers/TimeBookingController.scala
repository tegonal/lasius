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

import akka.util.Timeout
import controllers.TimeBookingController.DAY_NAMES
import core.{DBSession, SystemServices}
import domain.UserTimeBookingAggregate._
import models._
import org.joda.time.{DateTimeConstants, Days, LocalDate}
import play.api.cache.AsyncCacheApi
import play.api.mvc.{Action, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{BookingHistoryRepository, PublicHolidayRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

class TimeBookingController @Inject() (
    controllerComponents: ControllerComponents,
    override val authConfig: AuthConfig,
    override val cache: AsyncCacheApi,
    override val reactiveMongoApi: ReactiveMongoApi,
    override val systemServices: SystemServices,
    val bookingHistoryRepository: BookingHistoryRepository,
    val publicHolidayRepository: PublicHolidayRepository)(implicit
    ec: ExecutionContext)
    extends BaseLasiusController(controllerComponents) {

  override val supportTransaction: Boolean = systemServices.supportTransaction

  implicit val timeout: Timeout = systemServices.timeout

  def stopProjectBooking(
      orgId: OrganisationId,
      bookingId: BookingId): Action[StopProjectBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[StopProjectBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          systemServices.timeBookingViewService ! EndProjectBookingCommand(
            subject.userReference,
            userOrg.organisationReference,
            bookingId,
            request.body.end
          )
          success()
        }
    }

  def removeBooking(orgId: OrganisationId, bookingId: BookingId): Action[Unit] =
    HasUserRole(FreeUser, parse.empty, withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          for {
            _ <- bookingHistoryRepository
              .findByOrganisationAndId(userOrg.organisationReference, bookingId)
              .noneToFailed(
                s"Cannot find booking ${bookingId.value} in organisation ${userOrg.organisationReference.key}")
            _ = systemServices.timeBookingViewService ! RemoveBookingCommand(
              subject.userReference,
              userOrg.organisationReference,
              bookingId)
          } yield Ok
        }
    }

  def updateProjectBooking(
      orgId: OrganisationId,
      bookingId: BookingId): Action[UpdateProjectBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[UpdateProjectBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasOptionalProjectRole(userOrg,
                                 request.body.projectId,
                                 ProjectMember) { maybeUserProject =>
            for {
              _ <- request.body.start
                .flatMap(start =>
                  request.body.end.map(end =>
                    validateStartBeforeEnd(start, end)))
                .getOrElse(Future.successful(Ok))
              _ <- bookingHistoryRepository
                .findByOrganisationAndId(userOrg.organisationReference,
                                         bookingId)
                .noneToFailed(
                  s"Cannot find booking ${bookingId.value} in organisation ${userOrg.organisationReference.key}")
              command <- request.body
                .toCommand(bookingId,
                           userOrg.organisationReference,
                           maybeUserProject.map(_.projectReference))
              _ = systemServices.timeBookingViewService ! command
            } yield Ok
          }
        }
    }

  def changeProjectBookingStart(
      orgId: OrganisationId,
      bookingId: BookingId): Action[ProjectBookingChangeStartRequest] =
    HasUserRole(FreeUser,
                validateJson[ProjectBookingChangeStartRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) {
          userOrganisation =>
            systemServices.timeBookingViewService ! ChangeStartTimeOfBooking(
              subject.userReference,
              userOrganisation.organisationReference,
              bookingId,
              request.body.newStart)
            success()
        }
    }

  def startOrAddProjectBooking(
      orgId: OrganisationId): Action[StartOrAddProjectBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[StartOrAddProjectBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          HasProjectRole(userOrg, request.body.projectId, ProjectMember) {
            userProject =>
              for {
                _ <- request.body.end.fold(success())(
                  validateStartBeforeEnd(request.body.start, _))
                command <- request.body
                  .toCommand(userOrg.organisationReference,
                             userProject.projectReference)
                _ = systemServices.timeBookingViewService ! command
              } yield Ok
          }
        }
    }

  def addAbsenceBooking(
      orgId: OrganisationId): Action[AddAbsenceBookingRequest] =
    HasUserRole(FreeUser,
                validateJson[AddAbsenceBookingRequest],
                withinTransaction = false) {
      implicit dbSession => implicit subject => user => implicit request =>
        HasOrganisationRole(user, orgId, OrganisationMember) { userOrg =>
          for {
            _ <- request.body.end.fold(success())(
              validateStartBeforeEnd(request.body.start, _))
            command <- request.body.toCommand(userOrg.organisationReference)
            end <- Future {
              request.body.end.orElse(request.body.duration.map(duration =>
                request.body.start.plus(duration)))
            }.noneToFailed("Either end or duration must be specified")

            startDate = request.body.start.toLocalDate
            noOfDays  = Days.daysBetween(startDate, end.toLocalDate).getDays + 1
            bookingDates         = (0 until noOfDays).map(startDate.plusDays)
            bookingDatesWeekDays = bookingDates.map(_.getDayOfWeek)

            // verify plans to work on those days
            _ <- validateBookedAbsenceDaysAreWorkingDays(userOrg,
                                                         bookingDatesWeekDays)

            // validate public holiday bookings
            _ <- request.body.bookingType match {
              case PublicHolidayBooking =>
                validateBookingPublicHolidaysExists(userOrg, bookingDates)
              case _ => success()
            }

            _ = systemServices.timeBookingViewService ! command
          } yield Ok
        }
    }

  /** Validate for all booked week days a planned working days entry > 0 hours
    * exist
    */
  private def validateBookedAbsenceDaysAreWorkingDays(
      userOrg: UserOrganisation,
      bookingDatesWeekDays: Seq[Int]): Future[Unit] =
    Future {
      bookingDatesWeekDays
        .filter {
          case DateTimeConstants.MONDAY =>
            userOrg.plannedWorkingHours.monday == 0
          case DateTimeConstants.TUESDAY =>
            userOrg.plannedWorkingHours.tuesday == 0
          case DateTimeConstants.WEDNESDAY =>
            userOrg.plannedWorkingHours.wednesday == 0
          case DateTimeConstants.THURSDAY =>
            userOrg.plannedWorkingHours.thursday == 0
          case DateTimeConstants.FRIDAY =>
            userOrg.plannedWorkingHours.friday == 0
          case DateTimeConstants.SATURDAY =>
            userOrg.plannedWorkingHours.saturday == 0
          case DateTimeConstants.SUNDAY =>
            userOrg.plannedWorkingHours.sunday == 0
        }
        .map(day => DAY_NAMES(day - 1))
        .pipe { list =>
          if (list.isEmpty) {
            None
          } else {
            Some(list)
          }
        }
    }.someToFailed(days =>
      s"Tried to add absence booking to the following non-working day(s) ${days
        .mkString(",")}")

  /** Validate for public holiday bookings that all booked days exists as public
    * holiday within the organisation
    */
  private def validateBookingPublicHolidaysExists(
      userOrg: UserOrganisation,
      bookingDatesList: Seq[LocalDate])(implicit
      dbSession: DBSession): Future[Unit] = {
    for {
      publicHolidays <- publicHolidayRepository.findByOrganisationAndDateRange(
        userOrg.organisationReference,
        bookingDatesList.head,
        bookingDatesList.last)
      publicHolidayDates = publicHolidays.map(_.date)
      _ <- validate(
        publicHolidayDates.containsSlice(bookingDatesList),
        s"The following dates of the public holiday booking ${bookingDatesList.filterNot(publicHolidayDates.contains).mkString(",")} do not match an existing public holiday"
      )
    } yield ()
  }
}

object TimeBookingController {
  val DAY_NAMES: List[String] = List(
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday"
  )
}
