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

package domain

import actors.ClientReceiver
import akka.PersistentActorTestScope
import akka.actor._
import akka.testkit.TestProbe
import core.{DBSession, MockServices, SystemServices}
import domain.AggregateRoot.Initialize
import domain.UserTimeBookingAggregate._
import models.LocalDateTimeWithTimeZone.DateTimeHelper
import models._
import mongo.EmbedMongo
import org.joda.time.{DateTime, Duration}
import org.mockito.ArgumentMatchers
import org.specs2.mock._
import org.specs2.mock.mockito.MockitoMatchers
import org.specs2.specification.core.Fragments
import play.api.libs.json.{Format, Writes}
import play.api.test.PlaySpecification
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._
import util.MockAwaitable

import scala.concurrent._

class UserTimeBookingAggregateSpec
    extends PlaySpecification
    with Mockito
    with MockAwaitable
    with MockitoMatchers
    with EmbedMongo
    with PersistentActorTestScope {

  val clientReceiver = mock[ClientReceiver]

  "UserTimeBookingAggregate RemoveBooking" should {
    "remove existing booking" >> {
      Fragments.foreach(BookingType.values) { bookingType =>
        s"bookingType=$bookingType" >> new UserTimeBookingAggregateMock {
          val booking = BookingV3(
            id = BookingId(),
            bookingType = bookingType,
            start = DateTime.now().toLocalDateTimeWithZone,
            end = None,
            duration = new Duration(0),
            userReference = userReference,
            organisationReference = organisationReference,
            projectReference = bookingType match {
              case ProjectBooking => Some(projectReference)
              case _              => None
            },
            tags = Set()
          )

          actorRef ! Initialize(
            UserTimeBooking(userReference, None, Seq(booking)))

          // execute
          probe.send(actorRef,
                     RemoveBookingCommand(userReference,
                                          booking.organisationReference,
                                          booking.id))

          // verify
          probe.expectMsg(UserTimeBooking(userReference, None, Seq()))
          stream.expectMsg(UserTimeBookingRemovedV3(booking))

          there.was(
            one(bookingHistoryRepository).remove(ArgumentMatchers.eq(booking))(
              any[DBSession]))
        }
      }
    }

    "not publish event if booking does not exist" in new UserTimeBookingAggregateMock {
      actorRef ! Initialize(UserTimeBooking(userReference, None, Seq()))
      // execute
      probe.send(
        actorRef,
        RemoveBookingCommand(userReference, organisationReference, BookingId()))

      // verify
      probe.expectNoMessage()
      stream.expectNoMessage()

      there.was(
        no(bookingHistoryRepository)
          .remove(any[BookingV3])(any[DBSession]))
    }
  }

  "UserTimeBookingAggregate AddBooking" should {
    "add booking with end date will calculate duration" >> {
      Fragments.foreach(BookingType.values) { bookingType =>
        s"bookingType=$bookingType" >> new UserTimeBookingAggregateMock {
          val start = DateTime.now()
          val end   = start.plusHours(2)

          val bookingEvent = UserTimeBookingAddedV3(
            id = BookingId(),
            bookingType = bookingType,
            userReference = userReference,
            organisationReference = organisationReference,
            projectReference = bookingType match {
              case ProjectBooking => Some(projectReference)
              case _              => None
            },
            tags = Set(),
            start = start,
            endOrDuration = Left(end)
          )
          val booking = bookingEvent.toBooking

          actorRef ! Initialize(UserTimeBooking(userReference, None, Seq()))

          // execute
          probe.send(
            actorRef,
            AddBookingCommand(
              bookingType = bookingType,
              userReference = userReference,
              organisationReference = booking.organisationReference,
              projectReference = bookingType match {
                case ProjectBooking => Some(projectReference)
                case _              => None
              },
              tags = Set(),
              start = start,
              endOrDuration = Left(end)
            )
          )

          // verify
          probe.expectMsgPF() {
            case UserTimeBooking(`userReference`, None, Seq(newBooking)) =>
              newBooking must beLikeIgnoringId(booking)
          }
          stream.expectMsgPF() { case e: UserTimeBookingAddedV3 =>
            e must beLikeIgnoringId(bookingEvent)
          }

          there.was(
            one(bookingHistoryRepository)
              .upsert(any[BookingV3])(any[Writes[BookingId]], any[DBSession]))
        }
      }
    }

    "add booking with duration" >> {
      Fragments.foreach(BookingType.values) { bookingType =>
        s"bookingType=$bookingType" >> new UserTimeBookingAggregateMock {
          val start    = DateTime.now()
          val duration = new Duration(2000)

          val bookingEvent = UserTimeBookingAddedV3(
            id = BookingId(),
            bookingType = bookingType,
            userReference = userReference,
            organisationReference = organisationReference,
            projectReference = bookingType match {
              case ProjectBooking => Some(projectReference)
              case _              => None
            },
            tags = Set(),
            start = start,
            endOrDuration = Right(duration)
          )
          val booking = bookingEvent.toBooking

          actorRef ! Initialize(UserTimeBooking(userReference, None, Seq()))

          // execute
          probe.send(
            actorRef,
            AddBookingCommand(
              bookingType = bookingType,
              userReference = userReference,
              organisationReference = booking.organisationReference,
              projectReference = bookingType match {
                case ProjectBooking => Some(projectReference)
                case _              => None
              },
              tags = Set(),
              start = start,
              endOrDuration = Right(duration)
            )
          )

          // verify
          probe.expectMsgPF() {
            case UserTimeBooking(`userReference`, None, Seq(newBooking)) =>
              newBooking must beLikeIgnoringId(booking)
          }
          stream.expectMsgPF() { case e: UserTimeBookingAddedV3 =>
            e must beLikeIgnoringId(bookingEvent)
          }

          there.was(
            one(bookingHistoryRepository)
              .upsert(any[BookingV3])(any[Writes[BookingId]], any[DBSession]))
        }
      }
    }
  }

  "UserTimeBookingAggregate StartProjectBooking" should {
    "starting a new booking will stop currently running booking" in new UserTimeBookingAggregateMock {
      val currentBooking =
        UserTimeBookingStartedV3(
          id = BookingId(),
          start = DateTime.now.minusHours(2).toLocalDateTimeWithZone,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = projectReference,
          tags = Set()
        )
      val newBooking = UserTimeBookingStartedV3(
        id = BookingId(),
        start = DateTime.now.toLocalDateTimeWithZone,
        userReference = userReference,
        organisationReference = organisationReference,
        projectReference = projectReference,
        tags = Set()
      )
      val closedBooking = currentBooking.toBooking(Left(newBooking.start))

      actorRef ! Initialize(
        UserTimeBooking(userReference, Some(currentBooking), Seq()))

      // execute
      probe.send(
        actorRef,
        StartProjectBookingCommand(userReference,
                                   newBooking.organisationReference,
                                   newBooking.projectReference.get,
                                   newBooking.tags,
                                   newBooking.start.toDateTime)
      )

      // verify
      probe.expectMsg(UserTimeBooking(userReference, None, Seq(closedBooking)))
      probe.expectMsgPF() {
        case UserTimeBooking(_, Some(newCurrentBooking), Seq(booking)) =>
          newCurrentBooking must beLikeIgnoringId(newBooking)

          booking must beEqualTo(closedBooking)
      }

      stream.expectMsg(UserTimeBookingStoppedV3(closedBooking))
      stream.expectMsgPF() { case e: UserTimeBookingStartedV3 =>
        e must beLikeIgnoringId(newBooking)
      }

      // add current booking to repository
      there.was(
        one(bookingHistoryRepository)
          .upsert(ArgumentMatchers.eq(closedBooking))(any[Writes[BookingId]],
                                                      any[DBSession]))
    }

    "Start new booking" in new UserTimeBookingAggregateMock {
      val newBooking =
        UserTimeBookingStartedV3(
          id = BookingId(),
          start = DateTime.now.toLocalDateTimeWithZone,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = projectReference,
          tags = Set()
        )

      actorRef ! Initialize(UserTimeBooking(userReference, None, Seq()))

      // execute
      probe.send(
        actorRef,
        StartProjectBookingCommand(userReference,
                                   newBooking.organisationReference,
                                   newBooking.projectReference.get,
                                   newBooking.tags,
                                   newBooking.start.toDateTime)
      )

      // verify
      probe.expectMsgPF() {
        case UserTimeBooking(_, Some(currentBooking), Seq()) =>
          currentBooking must beLikeIgnoringId(newBooking)
      }
      stream.expectMsgPF() { case e: UserTimeBookingStartedV3 =>
        e must beLikeIgnoringId(newBooking)
      }
    }
  }

  "UserTimeBookingAggregate EndBooking" should {
    "don't stop booking if not the same id" in new UserTimeBookingAggregateMock {
      system.eventStream.subscribe(stream.ref,
                                   classOf[UserTimeBookingRemovedV3])
      val currentBooking =
        UserTimeBookingStartedV3(
          id = BookingId(),
          start = DateTime.now.minusHours(2).toLocalDateTimeWithZone,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = projectReference,
          tags = Set()
        )

      actorRef ! Initialize(
        UserTimeBooking(userReference, Some(currentBooking), Seq()))

      // execute
      probe.send(actorRef,
                 EndProjectBookingCommand(userReference,
                                          organisationReference,
                                          BookingId(),
                                          DateTime.now))

      // verify
      probe.expectNoMessage()
      stream.expectNoMessage()

      // add current booking to repository
      there.was(
        no(bookingHistoryRepository)
          .bulkInsert(any[List[BookingV3]])(any[DBSession]))
    }

    "stop booking with provided enddate" in new UserTimeBookingAggregateMock {
      val currentBooking =
        UserTimeBookingStartedV3(
          id = BookingId(),
          DateTime.now.minusHours(2).toLocalDateTimeWithZone,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = projectReference,
          tags = Set()
        )
      val date = DateTime.now
      val closedBooking =
        currentBooking.toBooking(Left(date.toLocalDateTimeWithZone))

      actorRef ! Initialize(
        UserTimeBooking(userReference, Some(currentBooking), Seq()))

      // execute
      probe.send(actorRef,
                 EndProjectBookingCommand(userReference,
                                          organisationReference,
                                          currentBooking.id,
                                          date))

      // verify
      probe.expectMsg(UserTimeBooking(userReference, None, Seq(closedBooking)))
      stream.expectMsg(UserTimeBookingStoppedV3(closedBooking))

      // add current booking to repository
      there.was(
        one(bookingHistoryRepository)
          .upsert(ArgumentMatchers.eq(closedBooking))(any[Writes[BookingId]],
                                                      any[DBSession]))
    }

    "stop booking with enddate in future" in new UserTimeBookingAggregateMock {
      val currentBooking =
        UserTimeBookingStartedV3(
          id = BookingId(),
          DateTime.now.minusHours(2).toLocalDateTimeWithZone,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = projectReference,
          tags = Set()
        )
      val date = DateTime.now.plusHours(2)
      val closedBooking =
        currentBooking.toBooking(Left(date.toLocalDateTimeWithZone))

      actorRef ! Initialize(
        UserTimeBooking(userReference, Some(currentBooking), Seq()))

      // execute
      probe.send(actorRef,
                 EndProjectBookingCommand(userReference,
                                          organisationReference,
                                          currentBooking.id,
                                          date))

      // verify
      probe.expectMsg(UserTimeBooking(userReference, None, Seq(closedBooking)))
      stream.expectMsg(UserTimeBookingStoppedV3(closedBooking))

      // add current booking to repository
      there.was(
        one(bookingHistoryRepository)
          .upsert(ArgumentMatchers.eq(closedBooking))(any[Writes[BookingId]],
                                                      any[DBSession]))
    }
  }

  "UserTimeBookingAggregate UserTimeBookingEdited" should {
    "update currently running booking" in new UserTimeBookingAggregateMock {
      val currentBooking = {
        UserTimeBookingStartedV3(
          id = BookingId(),
          start = DateTime.now.minusHours(2).toLocalDateTimeWithZone,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = projectReference,
          tags = Set()
        )
      }
      val editedProject = Some(EntityReference(ProjectId(), "proj2"))
      val editedBooking = currentBooking.copy(
        projectReference = editedProject,
        bookingHash =
          BookingHash.createHash(editedProject, currentBooking.tags))

      actorRef ! Initialize(
        UserTimeBooking(userReference, Some(currentBooking), Seq()))

      // execute
      probe.send(
        actorRef,
        UpdateBookingCommand(
          userReference = userReference,
          organisationReference = editedBooking.organisationReference,
          bookingId = currentBooking.id,
          projectReference = editedBooking.projectReference,
          tags = Some(editedBooking.tags),
          start = Some(editedBooking.start.toDateTime),
          endOrDuration = None
        )
      )

      // verify
      probe.expectMsgPF() {
        case UserTimeBooking(_, Some(newCurrentBooking), Seq()) =>
          newCurrentBooking must beEqualTo(editedBooking)
      }
      stream.expectMsg(
        UserTimeBookingInProgressEdited(currentBooking, editedBooking))
    }

    s"updating start will recalculate duration if end is provided" >> {
      Fragments.foreach(BookingType.values) { bookingType =>
        s"bookingType=$bookingType" >> {
          new UserTimeBookingAggregateMock {
            val end      = DateTime.now()
            val start    = end.minusHours(2)
            val newStart = start.minusHours(2)
            val currentBooking = BookingV3(
              id = BookingId(),
              bookingType = bookingType,
              start = start.toLocalDateTimeWithZone,
              end = Some(end.toLocalDateTimeWithZone),
              duration = new Duration(start, end),
              userReference = userReference,
              organisationReference = organisationReference,
              projectReference = bookingType match {
                case ProjectBooking => Some(projectReference)
                case _              => None
              },
              tags = Set()
            )
            val expectedModifiedBooking =
              currentBooking.copy(start = newStart.toLocalDateTimeWithZone,
                                  duration = new Duration(newStart, end))

            bookingHistoryRepository
              .updateBooking(ArgumentMatchers.eq(expectedModifiedBooking))(
                any[Format[BookingV3]],
                any[DBSession])
              .returns(Future.successful(true))

            actorRef ! Initialize(
              UserTimeBooking(userReference, None, Seq(currentBooking)))

            // execute
            probe.send(
              actorRef,
              UpdateBookingCommand(
                userReference = userReference,
                organisationReference = currentBooking.organisationReference,
                bookingId = currentBooking.id,
                projectReference = None,
                tags = None,
                start = Some(newStart),
                endOrDuration = None
              )
            )

            // verify
            probe.expectMsg(
              UserTimeBooking(userReference,
                              None,
                              Seq(expectedModifiedBooking)))
            stream.expectMsg(
              UserTimeBookingEditedV4(currentBooking, expectedModifiedBooking))

            there.was(
              one(bookingHistoryRepository)
                .updateBooking(ArgumentMatchers.eq(expectedModifiedBooking))(
                  any[Format[BookingV3]],
                  any[DBSession]))
          }
        }
      }
    }

    s"update duration will delete existing end date" >> {
      Fragments.foreach(BookingType.values) { bookingType =>
        s"bookingType=$bookingType" >> {
          new UserTimeBookingAggregateMock {
            val end         = DateTime.now()
            val start       = end.minusHours(2)
            val newDuration = new Duration(1234)
            val currentBooking = BookingV3(
              id = BookingId(),
              bookingType = bookingType,
              start = start.toLocalDateTimeWithZone,
              end = Some(end.toLocalDateTimeWithZone),
              duration = new Duration(start, end),
              userReference = userReference,
              organisationReference = organisationReference,
              projectReference = bookingType match {
                case ProjectBooking => Some(projectReference)
                case _              => None
              },
              tags = Set()
            )
            val expectedModifiedBooking =
              currentBooking.copy(end = None, duration = newDuration)

            bookingHistoryRepository
              .updateBooking(ArgumentMatchers.eq(expectedModifiedBooking))(
                any[Format[BookingV3]],
                any[DBSession])
              .returns(Future.successful(true))

            actorRef ! Initialize(
              UserTimeBooking(userReference, None, Seq(currentBooking)))

            // execute
            probe.send(
              actorRef,
              UpdateBookingCommand(
                userReference = userReference,
                currentBooking.organisationReference,
                currentBooking.id,
                projectReference = None,
                tags = None,
                start = None,
                endOrDuration = Some(Right(newDuration))
              )
            )

            // verify
            probe.expectMsg(
              UserTimeBooking(userReference,
                              None,
                              Seq(expectedModifiedBooking)))
            stream.expectMsg(
              UserTimeBookingEditedV4(currentBooking, expectedModifiedBooking))

            there.was(
              one(bookingHistoryRepository)
                .updateBooking(ArgumentMatchers.eq(expectedModifiedBooking))(
                  any[Format[BookingV3]],
                  any[DBSession]))
          }
        }
      }
    }

    s"update with end date will recalculate duration" >> {
      Fragments.foreach(BookingType.values) { bookingType =>
        s"bookingType=$bookingType" >> {
          new UserTimeBookingAggregateMock {
            val start  = DateTime.now().minusHours(4)
            val newEnd = start.plusHours(3)
            val currentBooking = BookingV3(
              id = BookingId(),
              bookingType = bookingType,
              start = start.toLocalDateTimeWithZone,
              end = None,
              duration = new Duration(1234),
              userReference = userReference,
              organisationReference = organisationReference,
              projectReference = bookingType match {
                case ProjectBooking => Some(projectReference)
                case _              => None
              },
              tags = Set()
            )
            val expectedModifiedBooking =
              currentBooking.copy(end = Some(newEnd.toLocalDateTimeWithZone),
                                  duration = new Duration(start, newEnd))

            bookingHistoryRepository
              .updateBooking(ArgumentMatchers.eq(expectedModifiedBooking))(
                any[Format[BookingV3]],
                any[DBSession])
              .returns(Future.successful(true))

            actorRef ! Initialize(
              UserTimeBooking(userReference, None, Seq(currentBooking)))

            // execute
            probe.send(
              actorRef,
              UpdateBookingCommand(
                userReference = userReference,
                organisationReference = currentBooking.organisationReference,
                bookingId = currentBooking.id,
                projectReference = None,
                tags = None,
                start = None,
                endOrDuration = Some(Left(newEnd))
              )
            )

            // verify
            probe.expectMsg(
              UserTimeBooking(userReference,
                              None,
                              Seq(expectedModifiedBooking)))
            stream.expectMsg(
              UserTimeBookingEditedV4(currentBooking, expectedModifiedBooking))

            there.was(
              one(bookingHistoryRepository)
                .updateBooking(ArgumentMatchers.eq(expectedModifiedBooking))(
                  any[Format[BookingV3]],
                  any[DBSession]))
          }
        }
      }
    }
  }

  "UserTimeBookingAggregate UserTimeBookingStartTimeChanged" should {
    "Move start time of booking in progress" in new UserTimeBookingAggregateMock {
      val start    = DateTime.now.minusHours(2)
      val newStart = start.minusHours(4)

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])
      val currentBooking =
        UserTimeBookingStartedV3(
          id = BookingId(),
          start = start.toLocalDateTimeWithZone,
          userReference = userReference,
          organisationReference = organisationReference,
          projectReference = projectReference,
          tags = Set()
        )
      val adjustedBooking =
        currentBooking.copy(start = newStart.toLocalDateTimeWithZone)

      actorRef ! Initialize(
        UserTimeBooking(userReference, Some(currentBooking), Seq()))

      // execute
      probe.send(actorRef,
                 ChangeStartTimeOfBooking(userReference,
                                          organisationReference,
                                          currentBooking.id,
                                          newStart))

      // verify
      probe.expectMsg(
        UserTimeBooking(userReference, Some(adjustedBooking), Seq()))
      stream.expectMsg(
        UserTimeBookingStartTimeChanged(adjustedBooking.id, start, newStart))
    }

    "do nothing if booking is not in progress" in new UserTimeBookingAggregateMock {
      val start    = DateTime.now.minusHours(2)
      val end      = start.plusHours(3)
      val newStart = start.minusHours(4)

      system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])
      val currentBooking = BookingV3(
        id = BookingId(),
        start = start.toLocalDateTimeWithZone,
        end = Some(end.toLocalDateTimeWithZone),
        duration = new Duration(start, end),
        userReference = userReference,
        organisationReference = organisationReference,
        projectReference = projectReference,
        tags = Set()
      )

      actorRef ! Initialize(
        UserTimeBooking(userReference, None, Seq(currentBooking)))

      // execute
      probe.send(actorRef,
                 ChangeStartTimeOfBooking(userReference,
                                          organisationReference,
                                          currentBooking.id,
                                          newStart))

      // verify
      probe.expectNoMessage()
      stream.expectNoMessage()
    }
  }

  private def beLikeIgnoringId(booking: UserTimeBookingStartedV3) =
    beLike[UserTimeBookingStartedV3] {
      case UserTimeBookingStartedV3(_,
                                    booking.bookingType,
                                    booking.start,
                                    booking.userReference,
                                    booking.organisationReference,
                                    booking.projectReference,
                                    booking.tags,
                                    booking.bookingHash) =>
        ok
      case notMatched =>
        ko(s"Expected:$booking, but received ignoring id $notMatched")
    }

  private def beLikeIgnoringId(booking: UserTimeBookingAddedV3) =
    beLike[UserTimeBookingAddedV3] {
      case UserTimeBookingAddedV3(_,
                                  booking.bookingType,
                                  booking.userReference,
                                  booking.organisationReference,
                                  booking.projectReference,
                                  booking.tags,
                                  booking.start,
                                  booking.endOrDuration) =>
        ok
      case notMatched =>
        ko(s"Expected:$booking, but received ignoring id $notMatched")
    }

  private def beLikeIgnoringId(booking: BookingV3) =
    beLike[BookingV3] {
      case BookingV3(_,
                     booking.bookingType,
                     booking.start,
                     booking.end,
                     booking.duration,
                     booking.userReference,
                     booking.organisationReference,
                     booking.projectReference,
                     booking.tags,
                     booking.bookingHash) =>
        ok
      case notMatched =>
        ko(s"Expected:$booking, but received ignoring id $notMatched")
    }

  trait UserTimeBookingAggregateMock extends WithPersistentActorTestScope {
    val systemServices = new MockServices(system)
    val probe          = TestProbe()
    val stream         = TestProbe()
    val userReference =
      EntityReference(UserId(), "noob")
    val projectReference =
      EntityReference(ProjectId(), "proj")
    val organisationReference =
      EntityReference(OrganisationId(), "org")
    val bookingHistoryRepository = mockAwaitable[BookingHistoryRepository]
    val actorRef =
      system.actorOf(
        UserTimeBookingAggregateMock.props(systemServices,
                                           clientReceiver,
                                           bookingHistoryRepository,
                                           userReference,
                                           reactiveMongoApi))
    system.eventStream.subscribe(stream.ref, classOf[PersistedEvent])
  }
}

object UserTimeBookingAggregateMock {

  def props(systemServices: SystemServices,
            clientReceiver: ClientReceiver,
            bookingHistoryRepository: BookingHistoryRepository,
            userReference: EntityReference[UserId],
            reactiveMongoApi: ReactiveMongoApi) =
    Props(
      new UserTimeBookingAggregateMock(systemServices,
                                       clientReceiver,
                                       bookingHistoryRepository,
                                       userReference,
                                       reactiveMongoApi))
}

class UserTimeBookingAggregateMock(
    systemServices: SystemServices,
    clientReceiver: ClientReceiver,
    bookingHistoryRepository: BookingHistoryRepository,
    userReference: EntityReference[UserId],
    reactiveMongoApi: ReactiveMongoApi)
    extends UserTimeBookingAggregate(systemServices,
                                     clientReceiver,
                                     bookingHistoryRepository,
                                     userReference,
                                     reactiveMongoApi)
