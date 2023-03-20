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

package models

import play.api.libs.json.{Format, Json}
import reactivemongo.api.bson.{BSONDocumentReader, Macros}

case class BookingStatsCategory(year: Option[Int],
                                month: Option[Int],
                                week: Option[Int],
                                day: Option[Int])

object BookingStatsCategory {
  implicit val format: Format[BookingStatsCategory] =
    Json.format[BookingStatsCategory]
  implicit val reader: BSONDocumentReader[BookingStatsCategory] =
    Macros.reader[BookingStatsCategory]
}

case class BookingStats(category: BookingStatsCategory,
                        values: Seq[BookingStatsByCategory])

object BookingStats {
  implicit val format: Format[BookingStats] = Json.format[BookingStats]

  implicit val reader: BSONDocumentReader[BookingStats] =
    Macros.reader[BookingStats]
}
