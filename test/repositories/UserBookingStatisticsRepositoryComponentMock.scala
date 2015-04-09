package repositories

import org.specs2.mock.Mockito

object UserBookingStatisticsRepositoryComponentMock extends Mockito {
  lazy val bookingByCategoryRepository: BookingByCategoryRepository = mock[BookingByCategoryRepository]
  lazy val bookingByProjectRepository: BookingByProjectRepository = mock[BookingByProjectRepository]
  lazy val bookingByTagRepository: BookingByTagRepository = mock[BookingByTagRepository]
}

trait UserBookingStatisticsRepositoryComponentMock
  extends UserBookingStatisticsRepositoryComponent {

  val bookingByCategoryRepository = UserBookingStatisticsRepositoryComponentMock.bookingByCategoryRepository
  val bookingByProjectRepository = UserBookingStatisticsRepositoryComponentMock.bookingByProjectRepository
  val bookingByTagRepository = UserBookingStatisticsRepositoryComponentMock.bookingByTagRepository
}