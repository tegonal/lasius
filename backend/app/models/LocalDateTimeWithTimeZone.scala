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

import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import play.api.libs.json.{Format, Json}
import BaseFormat._

case class LocalDateTimeWithTimeZone(dateTime: LocalDateTime,
                                     zone: DateTimeZone) {
  def toDateTime: DateTime = dateTime.toDateTime(zone)

  def withTimeAtStartOfDay: LocalDateTime = {
    dateTime.toLocalDate.toDateTimeAtStartOfDay().toLocalDateTime
  }

}

object LocalDateTimeWithTimeZone {
  implicit class DateTimeHelper(self: DateTime) {
    def toLocalDateTimeWithZone: LocalDateTimeWithTimeZone =
      LocalDateTimeWithTimeZone(self.toLocalDateTime, self.getZone)
  }

  implicit val format: Format[LocalDateTimeWithTimeZone] =
    Json.format[LocalDateTimeWithTimeZone]
}
