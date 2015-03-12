package core

import akka.persistence._
import akka.actor._
import services._
import domain._
import domain.LoginStateAggregate._
import models.UserId
import core.Global._
import akka.event.EventStream

object LoginHandler {

   def subscribe(ref:ActorRef, eventStream: EventStream)= {
    eventStream.subscribe(ref, classOf[UserLoggedIn])
    eventStream.subscribe(ref, classOf[UserLoggedOut])
  }
  
  def props: Props = Props(new LoginHandler)
}

class LoginHandler  extends Actor with ActorLogging {

  import domain.UserTimeBookingAggregate._
  import domain.LoginStateAggregate._
  import services.UserService._ 
  
  val receive: Receive = {
    case UserLoggedIn(userId) =>
      handleLoggedIn(userId)
    case UserLoggedOut(userId) =>
     handleLoggedOut(userId)     
  }

  def handleLoggedIn(userId: UserId) = {
      log.debug(s"user logged in:$userId, start persistentViews")
      //initialize persistentviews
	  timeBookingHistoryViewService ! StartUserTimeBookingView(userId)
	  currentUserTimeBookingsViewService ! domain.views.CurrentUserTimeBookingsView.GetCurrentTimeBooking(userId)
	  timeBookingStatisticsViewService ! StartUserTimeBookingView(userId)
  }
  
  def handleLoggedOut(userId: UserId) = {
    log.debug(s"user logged in:$userId, stop persistentViews")
    
    //kill persistentviews
	timeBookingHistoryViewService ! StopUserView(userId)
	currentUserTimeBookingsViewService ! StopUserView(userId)
	timeBookingStatisticsViewService ! StopUserView(userId)
  }
}