package domain.views

import akka.actor._
import models._
import repositories._
import scala.concurrent.ExecutionContext.Implicits.global
import actors.ClientReceiverComponent

object CurrentTeamTimeBookingsView {
  
  case class TeamBookingState(timeBookings: Map[UserId, Option[CurrentUserTimeBooking]])
  
}

class CurrentTeamTimeBookingsView extends Actor with ActorLogging {
  self : BasicRepositoryComponent with ClientReceiverComponent =>
    
    import CurrentTeamTimeBookingsView._
    
  var teams: Map[TeamId, TeamBookingState] = Map()
  var user2Teams: Map[UserId, Seq[TeamId]] = Map()
    
  override def preStart() = {
    context.system.eventStream.subscribe(self, classOf[CurrentUserTimeBooking])    
  }
  
  def loadInitialTeams = {
    userRepository.findAll() map { users =>
      val userMap = users.map { user =>
        user.teams.map(t => (t.id, user.id))
      }.flatten
      
      teams = userMap.groupBy(_._1).map{case (t, v) => (t, TeamBookingState(v.map(x => (x._2, None)).toMap))}
      user2Teams = userMap.groupBy(_._2).map{case (t, v) => (t, v.map(_._1))}
    }
  }
  
  val receive:Receive = {
   case e:CurrentUserTimeBooking =>
     val userId = e.userId
     user2Teams.get(e.userId).map { userTeams => 
       //store latest event in map of team
       userTeams.map{ teamId => 
         val teamBookings  = (teams.get(teamId).map {state => 
           state.copy(timeBookings = state.timeBookings + (e.userId -> Some(e)))
         }.getOrElse(TeamBookingState(Map(userId -> Some(e)))))
         teams = teams + (teamId -> teamBookings)
         
         val teamMembers = teams.flatMap(_._2.timeBookings.map(_._1))
         
         //notify team
         clientReceiver ! (userId,  CurrentTeamTimebookings(teamMembers.toSet, teamBookings.timeBookings), teamMembers.toList)
       }
     }
  }
}