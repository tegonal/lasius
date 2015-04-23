package repositories

import org.mockito.MockingDetails
import org.specs2.mock.Mockito

trait UserBookingStatisticsRepositoryComponentMock extends UserBookingStatisticsRepositoryComponent with Mockito {
  val bookingByProjectRepository: BookingByProjectRepository = mock[BookingByProjectRepository]
  val bookingByCategoryRepository: BookingByCategoryRepository = mock[BookingByCategoryRepository]
  val bookingByTagRepository: BookingByTagRepository = mock[BookingByTagRepository]
}