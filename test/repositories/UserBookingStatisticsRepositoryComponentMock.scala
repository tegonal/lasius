/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package repositories

import org.mockito.MockingDetails
import org.specs2.mock.Mockito

class UserBookingStatisticsRepositoryComponentMockClass extends UserBookingStatisticsRepositoryComponentMock

trait UserBookingStatisticsRepositoryComponentMock extends UserBookingStatisticsRepositoryComponent with Mockito {
  val bookingByProjectRepository: BookingByProjectRepository = mock[BookingByProjectRepository]
  val bookingByCategoryRepository: BookingByCategoryRepository = mock[BookingByCategoryRepository]
  val bookingByTagRepository: BookingByTagRepository = mock[BookingByTagRepository]
}