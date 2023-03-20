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

import play.api.libs.json._

case class WorkingHours(monday: Float = 0,
                        tuesday: Float = 0,
                        wednesday: Float = 0,
                        thursday: Float = 0,
                        friday: Float = 0,
                        saturday: Float = 0,
                        sunday: Float = 0)

object WorkingHours {
  implicit val format: Format[WorkingHours] = Json.format[WorkingHours]
}
