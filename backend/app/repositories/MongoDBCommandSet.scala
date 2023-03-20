/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package repositories

object MongoDBCommandSet {
  final val GreaterThan         = "$gt"
  final val GreaterOrEqualsThan = "$gte"
  final val LowerThan           = "$lt"
  final val LowerOrEqualsThan   = "$lte"
  final val AddToSet            = "$addToSet"
  final val Pull                = "$pull"
  final val Push                = "$push"
  final val Set                 = "$set"
  final val Not                 = "$not"
  final val NotEquals           = "$ne"
  final val Or                  = "$or"
  final val And                 = "$and"
  final val Inc                 = "$inc"
  final val Equals              = "$eq"
}
