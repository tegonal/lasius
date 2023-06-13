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
import { differenceInMinutes, isBefore } from 'date-fns';

type Item = (ModelsBooking & {
  overlapsWithNext?: ModelsBooking;
  isMostRecent?: boolean;
  hasNextItem?: boolean;
  allowInsert?: boolean;
})[];

export const augmentBookingsList = (bookings: ModelsBooking[]): Item => {
  const sortedBookings = sortBookingsByDate(bookings);

  return sortedBookings.map((booking, index) => {
    const nextBooking = sortedBookings[index + 1];
    const isMostRecent = index === 0;
    const hasNextItem = index < sortedBookings.length - 1;

    if (nextBooking && booking.end && nextBooking.end) {
      const isOverlapping =
        nextBooking.end.dateTime !== booking.start.dateTime &&
        !isBefore(new Date(nextBooking.end.dateTime), new Date(booking.start.dateTime));

      const hasGap =
        differenceInMinutes(new Date(booking.start.dateTime), new Date(nextBooking.end.dateTime)) >
        1;

      return {
        ...booking,
        overlapsWithNext: isOverlapping ? nextBooking : undefined,
        isMostRecent,
        hasNextItem,
        allowInsert: hasGap,
      };
    }

    return {
      ...booking,
      isMostRecent,
      hasNextItem,
    };
  });
};
