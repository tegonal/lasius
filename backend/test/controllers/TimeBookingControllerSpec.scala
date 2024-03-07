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

import core._
import domain.UserTimeBookingAggregate.{
  AddBookingCommand,
  StartProjectBookingCommand,
  UpdateBookingCommand
}
import models.LocalDateTimeWithTimeZone._
import models._
import mongo.EmbedMongo
import org.joda.time.{DateTime, Duration}
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import play.api.mvc._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.BookingHistoryRepository
import util.MockAwaitable

import scala.concurrent.{ExecutionContext, Future}

class TimeBookingControllerSpec
    extends PlaySpecification
    with Mockito
    with Results
    with MockitoMatchers
    with TestApplication
    with EmbedMongo {

  "start project booking" should {

    "forbidden if for authenticated user project id does not exist" in new WithTimeBookingControllerMock {
      val projectId: ProjectId = ProjectId()
      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          StartOrAddProjectBookingRequest(
            projectId = projectId,
            tags = Set(SimpleTag(TagId("tag1"))),
            start = DateTime.now(),
          ))
      val result: Future[Result] =
        controller.startOrAddProjectBooking(controller.organisationId)(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "forbidden if for authenticated user organisation does not exist" in new WithTimeBookingControllerMock {
      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          StartOrAddProjectBookingRequest(
            projectId = controller.project.id,
            tags = Set(SimpleTag(TagId("tag1"))),
            start = DateTime.now()
          ))
      val organisationId: OrganisationId = OrganisationId()
      val result: Future[Result] =
        controller.startOrAddProjectBooking(organisationId)(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "successful, producing expected command" in new WithTimeBookingControllerMock {
      val tags: Set[Tag] = Set(SimpleTag(TagId("tag1")))
      val from: DateTime = DateTime.now()

      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          StartOrAddProjectBookingRequest(
            projectId = controller.project.id,
            tags = tags,
            start = from
          ))
      val result: Future[Result] =
        controller.startOrAddProjectBooking(controller.organisationId)(request)

      status(result) must equalTo(OK)
      systemServices.timeBookingViewServiceProbe.expectMsg(
        StartProjectBookingCommand(
          userReference = controller.userReference,
          organisationReference = controller.organisation.reference,
          projectReference = controller.project.reference,
          tags = tags,
          start = from
        )
      )
    }
  }

  "edit project booking" should {

    "forbidden if for authenticated user project id does not exist" in new WithTimeBookingControllerMock {
      val projectId: ProjectId = ProjectId()
      val request: FakeRequest[UpdateProjectBookingRequest] = FakeRequest()
        .withBody(
          UpdateProjectBookingRequest(
            projectId = Some(projectId)
          ))
      val result: Future[Result] =
        controller.updateProjectBooking(controller.organisationId, BookingId())(
          request)

      status(result) must equalTo(FORBIDDEN)
    }

    "forbidden if for authenticated user organisation does not exist" in new WithTimeBookingControllerMock {
      val request: FakeRequest[UpdateProjectBookingRequest] = FakeRequest()
        .withBody(
          UpdateProjectBookingRequest(
            projectId = Some(controller.project.id),
            tags = None,
            start = None,
            end = None
          ))
      val organisationId: OrganisationId = OrganisationId()
      val result: Future[Result] =
        controller.updateProjectBooking(organisationId, BookingId())(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "badrequest if end date is before start date" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.minusDays(1)
      val request: FakeRequest[UpdateProjectBookingRequest] = FakeRequest()
        .withBody(
          UpdateProjectBookingRequest(
            projectId = None,
            tags = None,
            start = Some(from),
            end = Some(to)
          ))
      val result: Future[Result] =
        controller.updateProjectBooking(controller.organisationId, BookingId())(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        "Start date needs to be before end date")
    }

    "badrequest providing end date and duration" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)

      val bookingId = BookingId()
      withDBSession() { implicit dbSession =>
        bookingHistoryRepository.upsert(
          BookingV3(
            id = bookingId,
            start = from.toLocalDateTimeWithZone,
            end = None,
            duration = new Duration(10),
            tags = Set(),
            userReference = controller.userReference,
            organisationReference = controller.organisation.reference,
            projectReference = controller.project.reference
          )
        )
      }.awaitResult()

      val request: FakeRequest[UpdateProjectBookingRequest] = FakeRequest()
        .withBody(
          UpdateProjectBookingRequest(
            projectId = None,
            tags = None,
            start = Some(from),
            end = Some(to),
            duration = Some(new Duration(1000))
          ))
      val result: Future[Result] =
        controller.updateProjectBooking(controller.organisationId, bookingId)(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        "Provided non mutual values for 'end_date' and 'duration'")
    }

    "badrequest if booking does not exist in provided organisation" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)
      val request: FakeRequest[UpdateProjectBookingRequest] = FakeRequest()
        .withBody(
          UpdateProjectBookingRequest(
            projectId = None,
            tags = None,
            start = Some(from),
            end = None,
            duration = Some(new Duration(1000))
          ))
      val bookingId = BookingId()
      val result: Future[Result] =
        controller.updateProjectBooking(controller.organisationId, bookingId)(
          request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        s"Cannot find booking ${bookingId.value} in organisation ${controller.organisation.key}")
    }

    "successful, producing expected command" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)

      val bookingId = BookingId()
      withDBSession() { implicit dbSession =>
        bookingHistoryRepository.upsert(
          BookingV3(
            id = bookingId,
            start = from.toLocalDateTimeWithZone,
            end = None,
            duration = new Duration(10),
            tags = Set(),
            userReference = controller.userReference,
            organisationReference = controller.organisation.reference,
            projectReference = controller.project.reference
          )
        )
      }.awaitResult()

      val request: FakeRequest[UpdateProjectBookingRequest] = FakeRequest()
        .withBody(
          UpdateProjectBookingRequest(
            projectId = None,
            tags = None,
            start = Some(from),
            end = Some(to)
          ))
      val result: Future[Result] =
        controller.updateProjectBooking(controller.organisationId, bookingId)(
          request)

      status(result) must equalTo(OK)
      systemServices.timeBookingViewServiceProbe.expectMsg(
        UpdateBookingCommand(
          userReference = controller.userReference,
          organisationReference = controller.organisation.reference,
          bookingId = bookingId,
          projectReference = None,
          tags = None,
          start = Some(from),
          endOrDuration = Some(Left(to))
        )
      )
    }
  }

  "add project booking" should {

    "forbidden if for authenticated user project id does not exist" in new WithTimeBookingControllerMock {
      val from: DateTime       = DateTime.now()
      val to: DateTime         = from.plusHours(1)
      val projectId: ProjectId = ProjectId()
      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          models.StartOrAddProjectBookingRequest(
            projectId = projectId,
            tags = Set(SimpleTag(TagId("tag1"))),
            start = from,
            end = Some(to)))
      val result: Future[Result] =
        controller.startOrAddProjectBooking(controller.organisationId)(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "forbidden if for authenticated user organisation does not exist" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)
      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          models.StartOrAddProjectBookingRequest(
            projectId = controller.project.id,
            tags = Set(SimpleTag(TagId("tag1"))),
            start = from,
            end = Some(to)))
      val organisationId: OrganisationId = OrganisationId()
      val result: Future[Result] =
        controller.startOrAddProjectBooking(organisationId)(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "badrequest if end date is before start date" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.minusHours(1)
      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          models.StartOrAddProjectBookingRequest(
            projectId = controller.project.id,
            tags = Set(SimpleTag(TagId("tag1"))),
            start = from,
            end = Some(to)))
      val result: Future[Result] =
        controller.startOrAddProjectBooking(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        "Start date needs to be before end date")
    }

    "badrequest providing end date and duration" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)
      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          models.StartOrAddProjectBookingRequest(
            projectId = controller.project.id,
            tags = Set(SimpleTag(TagId("tag1"))),
            start = from,
            end = Some(to),
            duration = Some(new Duration(1000))))
      val result: Future[Result] =
        controller.startOrAddProjectBooking(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        "Provided non mutual values for 'end_date' and 'duration'")
    }

    "successful with end date, producing expected command" in new WithTimeBookingControllerMock {
      val tags: Set[Tag] = Set(SimpleTag(TagId("tag1")))
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)

      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          StartOrAddProjectBookingRequest(
            projectId = controller.project.id,
            tags = tags,
            start = from,
            end = Some(to)
          ))
      val result: Future[Result] =
        controller.startOrAddProjectBooking(controller.organisationId)(request)

      status(result) must equalTo(OK)
      systemServices.timeBookingViewServiceProbe.expectMsg(
        AddBookingCommand(
          bookingType = ProjectBooking,
          userReference = controller.userReference,
          organisationReference = controller.organisation.reference,
          projectReference = Some(controller.project.reference),
          tags = tags,
          start = from,
          endOrDuration = Left(to)
        )
      )
    }

    "successful with duration, producing expected command" in new WithTimeBookingControllerMock {
      val tags: Set[Tag] = Set(SimpleTag(TagId("tag1")))
      val from: DateTime = DateTime.now()
      val duration       = new Duration(1000)

      val request: FakeRequest[StartOrAddProjectBookingRequest] = FakeRequest()
        .withBody(
          StartOrAddProjectBookingRequest(
            projectId = controller.project.id,
            tags = tags,
            start = from,
            duration = Some(duration)
          ))
      val result: Future[Result] =
        controller.startOrAddProjectBooking(controller.organisationId)(request)

      status(result) must equalTo(OK)
      systemServices.timeBookingViewServiceProbe.expectMsg(
        AddBookingCommand(
          bookingType = ProjectBooking,
          userReference = controller.userReference,
          organisationReference = controller.organisation.reference,
          projectReference = Some(controller.project.reference),
          tags = tags,
          start = from,
          endOrDuration = Right(duration)
        )
      )
    }
  }

  "add absence booking" should {

    "forbidden if for authenticated user organisation does not exist" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)
      val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
        .withBody(
          models.AddAbsenceBookingRequest(bookingType = HolidayBooking,
                                          tags = Set(SimpleTag(TagId("tag1"))),
                                          start = from,
                                          end = Some(to)))
      val organisationId: OrganisationId = OrganisationId()
      val result: Future[Result] =
        controller.addAbsenceBooking(organisationId)(request)

      status(result) must equalTo(FORBIDDEN)
    }

    "badrequest if end date is before start date" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.minusHours(1)
      val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
        .withBody(
          models.AddAbsenceBookingRequest(bookingType = HolidayBooking,
                                          tags = Set(SimpleTag(TagId("tag1"))),
                                          start = from,
                                          end = Some(to)))
      val result: Future[Result] =
        controller.addAbsenceBooking(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        "Start date needs to be before end date")
    }

    "badrequest providing end date and duration" in new WithTimeBookingControllerMock {
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)
      val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
        .withBody(
          models.AddAbsenceBookingRequest(bookingType = HolidayBooking,
                                          tags = Set(SimpleTag(TagId("tag1"))),
                                          start = from,
                                          end = Some(to),
                                          duration = Some(new Duration(1000))))
      val result: Future[Result] =
        controller.addAbsenceBooking(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        "Provided non mutual values for 'end_date' and 'duration'")
    }

    "successful with end date, producing expected command" in new WithTimeBookingControllerMock {
      val tags: Set[Tag] = Set(SimpleTag(TagId("tag1")))
      val from: DateTime = DateTime.now()
      val to: DateTime   = from.plusHours(1)

      val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
        .withBody(
          models.AddAbsenceBookingRequest(bookingType = HolidayBooking,
                                          tags = Set(SimpleTag(TagId("tag1"))),
                                          start = from,
                                          end = Some(to)))
      val result: Future[Result] =
        controller.addAbsenceBooking(controller.organisationId)(request)

      status(result) must equalTo(OK)
      systemServices.timeBookingViewServiceProbe.expectMsg(
        AddBookingCommand(
          bookingType = HolidayBooking,
          userReference = controller.userReference,
          organisationReference = controller.organisation.reference,
          projectReference = None,
          tags = tags,
          start = from,
          endOrDuration = Left(to)
        )
      )
    }

    "successful with duration, producing expected command" in new WithTimeBookingControllerMock {
      val tags: Set[Tag] = Set(SimpleTag(TagId("tag1")))
      val from: DateTime = DateTime.now()
      val duration       = new Duration(1000)

      val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
        .withBody(
          models.AddAbsenceBookingRequest(bookingType = PublicHolidayBooking,
                                          tags = Set(SimpleTag(TagId("tag1"))),
                                          start = from,
                                          duration = Some(duration)))
      val result: Future[Result] =
        controller.addAbsenceBooking(controller.organisationId)(request)

      status(result) must equalTo(OK)
      systemServices.timeBookingViewServiceProbe.expectMsg(
        AddBookingCommand(
          bookingType = PublicHolidayBooking,
          userReference = controller.userReference,
          organisationReference = controller.organisation.reference,
          projectReference = None,
          tags = tags,
          start = from,
          endOrDuration = Right(duration)
        )
      )
    }
  }

  trait WithTimeBookingControllerMock extends WithTestApplication {
    implicit val executionContext: ExecutionContext = inject[ExecutionContext]
    val systemServices = inject[SystemServices].asInstanceOf[MockServices]
    val authConfig     = inject[AuthConfig]
    val bookingHistoryRepository = inject[BookingHistoryRepository]

    val controller: TimeBookingController
      with SecurityControllerMock
      with MockCacheAware =
      TimeBookingControllerMock(systemServices,
                                authConfig,
                                reactiveMongoApi,
                                bookingHistoryRepository)
  }
}

object TimeBookingControllerMock extends MockAwaitable with Mockito {

  def apply(systemServices: SystemServices,
            authConfig: AuthConfig,
            reactiveMongoApi: ReactiveMongoApi,
            bookingHistoryRepository: BookingHistoryRepository)(implicit
      ec: ExecutionContext): TimeBookingController
    with SecurityControllerMock
    with MockCacheAware = {

    new TimeBookingController(
      Helpers.stubControllerComponents(),
      authConfig,
      MockCache,
      reactiveMongoApi,
      systemServices,
      bookingHistoryRepository) with SecurityControllerMock with MockCacheAware
  }
}
