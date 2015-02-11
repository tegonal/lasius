package com.tegonal.lasius.services

import com.greencatsoft.angularjs.injectable
import com.greencatsoft.angularjs.core.HttpService
import com.greencatsoft.angularjs.Factory
import com.greencatsoft.angularjs.inject
import scala.concurrent.Future

@injectable("$bookingService")
class BookingService(val http: HttpService) {
  require(http != null, "Missing argument 'http'.")
  
  def getCategories(userId: String): Future[Category] = flatten {
	 require(userId != null, "Missing argument 'userId'.")

    val future: Future[js.Any] = http.post(s"/api/todos/${userId}", Pickle.intoString(task))

    future
      .map(JSON.stringify(_))
      .map(Unpickle[Task].fromString(_))
  }
}

object BookingServiceFactory extends Factory[BookingService] {

  override val name = "$taskService"

  @inject
  var http: HttpService = _

  override def apply(): BookingService = new BookingService(http)
}