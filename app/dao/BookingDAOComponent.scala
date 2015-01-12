package dao

trait BookingDAOComponent {
  val bookingDAO: BookingDAO
  val structureDAO: StructureDAO
}

trait MongoBookingDAOComponent extends BookingDAOComponent {
  val bookingDAO = new BookingMongoDAO
  val structureDAO = new StructureMongoDAO
}