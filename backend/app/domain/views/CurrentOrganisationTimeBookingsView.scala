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

package domain.views

import actors._
import akka.actor._
import akka.pattern.StatusReply
import core.DBSupport
import models._
import org.joda.time.{Duration, LocalDate}
import play.modules.reactivemongo.ReactiveMongoApi
import repositories._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps

object CurrentOrganisationTimeBookingsView {

  case class OrganisationBookingState(
      timeBookings: Map[UserId, Option[CurrentUserTimeBooking]])

  case class GetCurrentOrganisationTimeBookings(orgId: OrganisationId)

  case object NoResultFound

  case object Initialize

  def props(userRepository: UserRepository,
            clientReceiver: ClientReceiver,
            reactiveMongoApi: ReactiveMongoApi,
            supportTransaction: Boolean): Props =
    Props(classOf[CurrentOrganisationTimeBookingsView],
          userRepository,
          clientReceiver,
          reactiveMongoApi,
          supportTransaction)
}

class CurrentOrganisationTimeBookingsView(
    userRepository: UserRepository,
    clientReceiver: ClientReceiver,
    override val reactiveMongoApi: ReactiveMongoApi,
    override val supportTransaction: Boolean)
    extends Actor
    with ActorLogging
    with DBSupport {

  import CurrentOrganisationTimeBookingsView._

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  var organisations: Map[OrganisationId, OrganisationBookingState] = Map()

  override def preStart(): Unit = {
    log.debug(
      s"CurrentOrganisationTimeBookingsView: preStart, register as listener, ${context.system}")
    context.system.eventStream.subscribe(this.self, classOf[OutEvent])
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(this.self, classOf[OutEvent])
    super.postStop()
  }

  private def loadInitialOrganisations(replyTo: ActorRef): Future[Any] = {
    log.debug(s"loadInitialOrganisations")
    withDBSession() { implicit dbSession =>
      userRepository.findAll().map { users =>
        log.debug(s"findAllUsers:$users")
        if (users.nonEmpty) {
          val userMap = users.filter(_.active).flatMap { user =>
            user.organisations.map(t =>
              (t.organisationReference, user.getReference()))
          }
          val today = LocalDate.now()

          organisations = userMap.groupBy(_._1).map { case (t, v) =>
            (t.id,
             OrganisationBookingState(
               v.map(x =>
                 (x._2.id,
                  Some(
                    CurrentUserTimeBooking(
                      x._2,
                      today,
                      None,
                      None,
                      Duration.ZERO
                    ))))
                 .toMap))
          }
          log.debug(s"loadInitialOrganisations: $organisations")
        }
        replyTo ! StatusReply.Ack
      }
    }
  }

  val receive: Receive = {
    case Initialize =>
      loadInitialOrganisations(sender())
    case e @ CurrentUserTimeBookingEvent(
          CurrentUserTimeBooking(userReference, _, maybeBooking, _, _)) =>
      val userId = userReference.id
      log.debug(
        s"CurrentOrganisationTimeBookingsView: received $maybeBooking for user ${userReference.key}")

      // first other booking of current user, if there is any
      organisations
        .find(_._2.timeBookings.exists(s =>
          s._1 == userId && s._2.flatMap(_.booking).isDefined))
        .foreach { case (orgId, state) =>
          state.timeBookings.get(userId).flatten.foreach {
            currentUserTimeBooking =>
              val newState =
                state.copy(timeBookings = state.timeBookings.updated(
                  userId,
                  Some(currentUserTimeBooking.copy(booking = None))))

              organisations = organisations.updated(orgId, newState)

              notifyOrganisations(userId,
                                  currentUserTimeBooking.day,
                                  orgId,
                                  newState)
          }
        }

      // append if new booking
      maybeBooking
        .foreach { booking =>
          val organisationBookings = organisations
            .get(booking.organisationReference.id)
            .fold(OrganisationBookingState(Map(userId -> Some(e.booking))))(
              state =>
                state.copy(timeBookings =
                  state.timeBookings + (userId -> Some(e.booking))))

          log.debug(
            s"Update state:${booking.organisationReference.id.value} - $organisationBookings")

          organisations =
            organisations.updated(booking.organisationReference.id,
                                  organisationBookings)

          // notify organisation
          notifyOrganisations(userId,
                              e.booking.day,
                              booking.organisationReference.id,
                              organisationBookings)
        }
      sender() ! StatusReply.Ack
    case GetCurrentOrganisationTimeBookings(orgId) =>
      log.debug(s"GetCurrentOrganisationTimeBookings:${orgId.value}")
      val s = sender()
      organisations
        .get(orgId)
        .map { orgBookings =>
          log.debug(
            s"GetCurrentOrganisationTimeBookings:${orgId.value} -> $orgBookings")
          s ! CurrentOrganisationTimeBookings(
            orgId,
            orgBookings.timeBookings.values.flatten.toSeq)
        }
        .getOrElse {
          log.debug(
            s"GetCurrentOrganisationTimeBookings:${orgId.value} -> NoResultsFound")
          s ! NoResultFound
        }
    case e =>
      log.debug(s"received unknown event: $e")
  }

  private def notifyOrganisations(userId: UserId,
                                  day: LocalDate,
                                  organisationId: OrganisationId,
                                  newState: OrganisationBookingState) = {
    val today = LocalDate.now()

    if (!today.isAfter(day)) {
      val orgMembers = newState.timeBookings.keys
      log.debug(s"Notify org members:$orgMembers -> $newState")
      clientReceiver ! (userId, CurrentOrganisationTimeBookings(
        organisationId,
        newState.timeBookings.values.toSeq.flatten), orgMembers.toList)
    }
  }
}
