package repositories

trait UserBookingStatisticsRepositoryComponent {

  val bookingByProjectRepository: BookingByProjectRepository
  val bookingByCategoryRepository: BookingByCategoryRepository
  val bookingByTagRepository: BookingByTagRepository
}

trait MongoUserBookingStatisticsRepositoryComponent extends UserBookingStatisticsRepositoryComponent {
  val bookingByProjectRepository = new BookingByProjectMongoRepository
  val bookingByCategoryRepository = new BookingByCategoryMongoRepository
  val bookingByTagRepository = new BookingByTagMongoRepository
}