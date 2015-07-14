package repositories

import org.specs2.mock.Mockito

class UserBookingHistoryRepositoryComponentMock extends UserBookingHistoryRepositoryComponent with Mockito {
  val bookingHistoryRepository = mock[BookingHistoryRepository]
}