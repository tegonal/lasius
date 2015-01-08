package dao

trait BasicDAOComponent {
  val userDAO: UserDAO
  val projectDAO: ProjectDAO
  val categoryDAO: CategoryDAO
}

trait MongoBasicDAOComponent extends BasicDAOComponent {
  val userDAO = new UserMongoDAO
  val projectDAO = new ProjectMongoDAO
  val categoryDAO = new CategoryMongoDAO
}