package repositories

trait UserDataRepositoryComponent {
  val userFavoritesRepository: UserFavoritesRepository
}

trait MongoUserDataRepositoryComponent extends UserDataRepositoryComponent {
  val userFavoritesRepository = new UserFavoritesMongoRepository
}