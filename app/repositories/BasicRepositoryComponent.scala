package repositories

trait BasicRepositoryComponent extends SecurityRepositoryComponent {
  val userRepository: UserRepository
}

trait MongoBasicRepositoryComponent extends BasicRepositoryComponent {
  val userRepository = new UserMongoRepository
}