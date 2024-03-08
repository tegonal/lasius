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
import org.joda.time.{DateTime, DateTimeConstants, Duration}
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.MockitoMatchers
import org.specs2.specification.core.Fragments
import play.api.mvc._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoApi
import repositories.{BookingHistoryRepository, PublicHolidayRepository}
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

      val bookingId: BookingId = BookingId()
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
      val request: FakeRequest[UpdateProjectBookingRequest] = FakeRequest()
        .withBody(
          UpdateProjectBookingRequest(
            projectId = None,
            tags = None,
            start = Some(from),
            end = None,
            duration = Some(new Duration(1000))
          ))
      val bookingId: BookingId = BookingId()
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

      val bookingId: BookingId = BookingId()
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

    "badrequest if adding an absence to a date-range including a day not having non-working hours" >> {
      Fragments.foreach(NonWorkingTimeBooking.values) { bookingType =>
        s"bookingType=$bookingType" >> new WithTimeBookingControllerMock {
          override val plannedWorkingHours: WorkingHours = WorkingHours(
            monday = 8,
            wednesday = 4
          )

          val from: DateTime = DateTime
            .now()
            .withHourOfDay(5)
            .withDayOfWeek(DateTimeConstants.MONDAY)
          val to: DateTime = from.plusDays(2).plusHours(3)

          val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
            .withBody(
              models.AddAbsenceBookingRequest(bookingType = bookingType,
                                              tags =
                                                Set(SimpleTag(TagId("tag1"))),
                                              start = from,
                                              end = Some(to)))
          val result: Future[Result] =
            controller.addAbsenceBooking(controller.organisationId)(request)

          status(result) must equalTo(BAD_REQUEST)
          contentAsString(result) must startWith(
            "Tried to add absence booking to the following non-working day(s) Tuesday")
        }
      }
    }

    "badrequest if adding a public holiday booking to a date-range not marked as public holiday in the org" in new WithTimeBookingControllerMock {
      override val plannedWorkingHours: WorkingHours = WorkingHours(
        monday = 8,
        tuesday = 6,
        wednesday = 4
      )

      val from: DateTime = DateTime
        .now()
        .withHourOfDay(5)
        .withDayOfWeek(DateTimeConstants.MONDAY)
      val to: DateTime = from.plusDays(2).plusHours(3)

      withDBSession() { implicit dbSession =>
        controller.publicHolidayRepository.bulkInsert(
          List(
            PublicHoliday(
              id = PublicHolidayId(),
              organisationReference = controller.organisation.reference,
              date = from.toLocalDate,
              year = from.getYear,
              name = "Start Day"
            ),
            PublicHoliday(
              id = PublicHolidayId(),
              organisationReference = controller.organisation.reference,
              date = to.toLocalDate,
              year = to.getYear,
              name = "End Day"
            )
          )
        )
      }
        .awaitResult()

      val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
        .withBody(
          models.AddAbsenceBookingRequest(bookingType = PublicHolidayBooking,
                                          tags = Set(SimpleTag(TagId("tag1"))),
                                          start = from,
                                          end = Some(to)))
      val result: Future[Result] =
        controller.addAbsenceBooking(controller.organisationId)(request)

      status(result) must equalTo(BAD_REQUEST)
      contentAsString(result) must startWith(
        s"The following dates of the public holiday booking ${from.plusDays(1).toLocalDate} do not match an existing public holiday")
    }

    "successful with end date, producing expected command" >> {
      Fragments.foreach(NonWorkingTimeBooking.values) { bookingType =>
        s"bookingType=$bookingType" >> new WithTimeBookingControllerMock {
          override val plannedWorkingHours: WorkingHours = WorkingHours(
            monday = 1,
            tuesday = 2,
            wednesday = 3,
            thursday = 4,
            friday = 5,
            saturday = 2.5f,
            sunday = 3.6f
          )

          val tags: Set[Tag] = Set(SimpleTag(TagId("tag1")))
          val from: DateTime = DateTime.now()
          val to: DateTime   = from.plusHours(1)

          // create public holiday
          if (bookingType == PublicHolidayBooking) {
            withDBSession() { implicit dbSession =>
              controller.publicHolidayRepository.upsert(
                PublicHoliday(
                  id = PublicHolidayId(),
                  organisationReference = controller.organisation.reference,
                  date = from.toLocalDate,
                  year = from.getYear,
                  name = "Start Day"
                )
              )
            }
              .awaitResult()
          }

          val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
            .withBody(
              models.AddAbsenceBookingRequest(bookingType = bookingType,
                                              tags = tags,
                                              start = from,
                                              end = Some(to)))
          val result: Future[Result] =
            controller.addAbsenceBooking(controller.organisationId)(request)

          status(result) must equalTo(OK)
          systemServices.timeBookingViewServiceProbe.expectMsg(
            AddBookingCommand(
              bookingType = bookingType,
              userReference = controller.userReference,
              organisationReference = controller.organisation.reference,
              projectReference = None,
              tags = tags,
              start = from,
              endOrDuration = Left(to)
            )
          )
        }
      }
    }

    "successful with duration, producing expected command" >> {
      Fragments.foreach(NonWorkingTimeBooking.values) { bookingType =>
        s"bookingType=$bookingType" >> new WithTimeBookingControllerMock {
          override val plannedWorkingHours: WorkingHours = WorkingHours(
            monday = 1,
            tuesday = 2,
            wednesday = 3,
            thursday = 4,
            friday = 5,
            saturday = 2.5f,
            sunday = 3.6f
          )

          val tags: Set[Tag] = Set(SimpleTag(TagId("tag1")))
          val from: DateTime = DateTime.now()
          val duration       = new Duration(1000)

          // create public holiday
          if (bookingType == PublicHolidayBooking) {
            withDBSession() { implicit dbSession =>
              controller.publicHolidayRepository.upsert(
                PublicHoliday(
                  id = PublicHolidayId(),
                  organisationReference = controller.organisation.reference,
                  date = from.toLocalDate,
                  year = from.getYear,
                  name = "Start Day"
                )
              )
            }
              .awaitResult()
          }

          val request: FakeRequest[AddAbsenceBookingRequest] = FakeRequest()
            .withBody(
              models.AddAbsenceBookingRequest(bookingType = bookingType,
                                              tags = tags,
                                              start = from,
                                              duration = Some(duration)))
          val result: Future[Result] =
            controller.addAbsenceBooking(controller.organisationId)(request)

          status(result) must equalTo(OK)
          systemServices.timeBookingViewServiceProbe.expectMsg(
            AddBookingCommand(
              bookingType = bookingType,
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
    }
  }

  trait WithTimeBookingControllerMock extends WithTestApplication {
    // overrides
    val plannedWorkingHours: WorkingHours = WorkingHours()

    implicit val executionContext: ExecutionContext = inject[ExecutionContext]
    val systemServices: MockServices =
      inject[SystemServices].asInstanceOf[MockServices]
    val authConfig: AuthConfig = inject[AuthConfig]
    val bookingHistoryRepository: BookingHistoryRepository =
      inject[BookingHistoryRepository]
    val publicHolidayRepository: PublicHolidayRepository =
      inject[PublicHolidayRepository]

    lazy val controller: TimeBookingController
      with SecurityControllerMock
      with MockCacheAware =
      TimeBookingControllerMock(
        systemServices = systemServices,
        authConfig = authConfig,
        reactiveMongoApi = reactiveMongoApi,
        bookingHistoryRepository = bookingHistoryRepository,
        publicHolidayRepository = publicHolidayRepository,
        plannedWorkingHours = plannedWorkingHours
      )
  }
}

object TimeBookingControllerMock extends MockAwaitable with Mockito {

  def apply(systemServices: SystemServices,
            authConfig: AuthConfig,
            reactiveMongoApi: ReactiveMongoApi,
            bookingHistoryRepository: BookingHistoryRepository,
            publicHolidayRepository: PublicHolidayRepository,
            plannedWorkingHours: WorkingHours = WorkingHours())(implicit
      ec: ExecutionContext): TimeBookingControllerMock = {

    new TimeBookingControllerMock(
      controllerComponents = Helpers.stubControllerComponents(),
      authConfig = authConfig,
      reactiveMongoApi = reactiveMongoApi,
      systemServices = systemServices,
      bookingHistoryRepository = bookingHistoryRepository,
      publicHolidayRepository = publicHolidayRepository,
      plannedWorkingHours = plannedWorkingHours)
      with SecurityControllerMock
      with MockCacheAware
  }
}

class TimeBookingControllerMock(
    controllerComponents: ControllerComponents,
    authConfig: AuthConfig,
    systemServices: SystemServices,
    reactiveMongoApi: ReactiveMongoApi,
    bookingHistoryRepository: BookingHistoryRepository,
    publicHolidayRepository: PublicHolidayRepository,
    override val plannedWorkingHours: WorkingHours
)(implicit ec: ExecutionContext)
    extends TimeBookingController(
      controllerComponents = controllerComponents,
      authConfig = authConfig,
      cache = MockCache,
      reactiveMongoApi = reactiveMongoApi,
      systemServices = systemServices,
      bookingHistoryRepository = bookingHistoryRepository,
      publicHolidayRepository = publicHolidayRepository
    )
    with SecurityControllerMock
    with MockCacheAware
