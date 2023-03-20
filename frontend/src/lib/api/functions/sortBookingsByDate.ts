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
import { orderBy } from 'lodash';
import { ExtendedHistoryBooking } from 'types/booking';

export const sortBookingsByDate = (bookings: ModelsBooking[]) => {
  if (bookings.length === 0) return [];
  return orderBy(bookings, (data) => data.start.dateTime, 'desc');
};

export const sortExtendedBookingsByDate = (bookings: ExtendedHistoryBooking[]) => {
  if (bookings.length === 0) return [];
  return orderBy(bookings, (data) => data.date, 'desc');
};
