package dao

trait BasicDAOComponent {
  val userDAO: UserDAO
  val structureDAO: StructureDAO
}

trait MongoBasicDAOComponent extends BasicDAOComponent {
  val userDAO = new UserMongoDAO
  val structureDAO = new StructureMongoDAO
}