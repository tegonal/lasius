package repositories

trait SecurityRepositoryComponent {
  val userRepository: UserRepository
}

trait MongoSecurityRepositoryComponent extends SecurityRepositoryComponent {
  val userRepository = new UserMongoRepository
}