package repositories

trait UserBookingHistoryRepositoryComponent {
  val bookingHistoryRepository: BookingHistoryRepository
}

trait MongoUserBookingHistoryRepositoryComponent extends UserBookingHistoryRepositoryComponent {
  val bookingHistoryRepository = new BookingHistoryMongoRepository
}