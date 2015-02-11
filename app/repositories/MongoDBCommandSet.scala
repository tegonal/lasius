package repositories

object MongoDBCommandSet {
  final val GreaterThan = "$gt";
  final val GreaterOrEqualsThan = "$gte";
  final val LowerThan = "$lt";
  final val LowerOrEqualsThan = "$lte";
  final val AddToSet = "$addToSet";
  final val Pull = "$pull";
  final val Set = "$set";
  final val Not = "$not"
  final val NotEquals = "$ne"
  final val Or = "$or"
  final val And = "$and"
}