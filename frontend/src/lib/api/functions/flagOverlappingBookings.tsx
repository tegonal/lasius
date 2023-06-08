/**
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Lasius.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 */

import { ModelsBooking } from 'lib/api/lasius';
import { sortBookingsByDate } from 'lib/api/functions/sortBookingsByDate';
import { isBefore } from 'date-fns';

export const flagOverlappingBookings = (
  bookings: ModelsBooking[]
): (ModelsBooking & { overlapsWithNext?: ModelsBooking })[] => {
  const sortedBookings = sortBookingsByDate(bookings);

  return sortedBookings.map((booking, index) => {
    const nextBooking = sortedBookings[index + 1];

    if (nextBooking && booking.end && nextBooking.end) {
      const isOverlapping = !isBefore(
        new Date(nextBooking.end.dateTime),
        new Date(booking.start.dateTime)
      );
      if (isOverlapping) {
        console.log('isOverlapping');
      }

      return {
        ...booking,
        overlapsWithNext: isOverlapping ? nextBooking : undefined,
      };
    }

    return booking;
  });
};
