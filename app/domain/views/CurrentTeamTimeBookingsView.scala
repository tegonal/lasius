/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package domain.views

import akka.actor._

import models._
import repositories._
import scala.concurrent.ExecutionContext.Implicits.global
import actors._
import scala.concurrent.duration._

object CurrentTeamTimeBookingsView {
  
  case class TeamBookingState(timeBookings: Map[UserId, Option[CurrentUserTimeBooking]])
  case class GetCurrentTeamTimeBookings(teamId: TeamId)
  case object NoResultFound
  case object Initialize
  
  def props: Props = Props(classOf[DefaultCurrentTeamTimeBookingsView])
}

class DefaultCurrentTeamTimeBookingsView extends CurrentTeamTimeBookingsView with DefaultClientReceiverComponent with MongoBasicRepositoryComponent

class CurrentTeamTimeBookingsView extends Actor with ActorLogging {
  self : BasicRepositoryComponent with ClientReceiverComponent =>
    
  import CurrentTeamTimeBookingsView._
    
  var teams: Map[TeamId, TeamBookingState] = Map()
  var user2Teams: Map[UserId, Seq[TeamId]] = Map()
    
  override def preStart() = {
    log.debug(s"CurrentTeamTimeBookingsView: preStart, register as listener, ${context.system}")
    context.system.eventStream.subscribe(self, classOf[OutEvent])
  }
  
  override def postStop() {
    context.system.eventStream.unsubscribe(this.self, classOf[OutEvent])
    super.postStop()
  }
  
  private def loadInitialTeams() = {
    log.debug(s"loadInitialTeams")
    userRepository.findAll() map { users =>
      log.debug(s"findAllUsers:$users")
      if (users.length > 0) {      
        val userMap = users.map { user =>
          user.teams.map(t => (t.id, user.id))
        }.flatten
        
        teams = userMap.groupBy(_._1).map{case (t, v) => (t, TeamBookingState(v.map(x => (x._2, None)).toMap))}
        user2Teams = userMap.groupBy(_._2).map{case (t, v) => (t, v.map(_._1))}
        log.debug(s"loadInitialTeams: $teams")
      }
      else {
        context.system.scheduler.scheduleOnce(1 second)(self ! Initialize)
      }
    }
  }
  
  val receive:Receive = {
   case Initialize => 
      loadInitialTeams()
   case e:CurrentUserTimeBookingEvent =>
     val userId = e.booking.userId
     log.debug(s"CurrentTeamTimeBookingsView: received $e")
     user2Teams.get(e.booking.userId).map { userTeams => 
       //store latest event in map of team
       userTeams.map{ teamId => 
         val teamBookings  = (teams.get(teamId).map {state => 
           state.copy(timeBookings = state.timeBookings + (e.booking.userId -> Some(e.booking)))
         }.getOrElse(TeamBookingState(Map(userId -> Some(e.booking)))))
         teams = teams + (teamId -> teamBookings)
         
         val teamMembers = teamBookings.timeBookings.map(_._1)
         
         //notify team
         log.debug(s"Notify team members:$teamMembers -> $teamBookings")
         clientReceiver ! (userId,  CurrentTeamTimeBookings(teamId, teamBookings.timeBookings.values.toSeq.flatten), teamMembers.toList)
       }
     }
   case GetCurrentTeamTimeBookings(teamId) => 
     //get current team time bookings by user
     log.debug(s"GetCurrenTeamTimebookings:$teamId")
     val s = sender
     teams.get(teamId).map { teamBookings => 
       log.debug(s"GetCurrenTeamTimebookings:$teamId -> $teamBookings")
       s ! CurrentTeamTimeBookings(teamId, teamBookings.timeBookings.values.toSeq.flatten)
     }.getOrElse {
       log.debug(s"GetCurrenTeamTimebookings:$teamId -> NoResultsFound")
       s ! NoResultFound
     }
   case e => 
    log.debug(s"received unknown event: $e")
  }  
}