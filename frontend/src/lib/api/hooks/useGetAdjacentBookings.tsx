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

import { apiTimespanDay } from 'lib/api/apiDateHandling';
import { useGetUserBookingListByOrganisation } from 'lib/api/lasius/user-bookings/user-bookings';
import { useMemo } from 'react';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { ModelsBooking } from 'lib/api/lasius';
import { sortBookingsByDate } from 'lib/api/functions/sortBookingsByDate';
import { formatISOLocale } from 'lib/dates';

export const useGetAdjacentBookings = (item: ModelsBooking | undefined) => {
  const { selectedOrganisationId } = useOrganisation();
  const { data: bookings } = useGetUserBookingListByOrganisation(
    selectedOrganisationId,
    apiTimespanDay(item?.start.dateTime || formatISOLocale(new Date()))
  );

  const sorted = sortBookingsByDate(bookings || []);
  const indexCurrent = sorted.findIndex((b) => b.id === item?.id);
  const data = useMemo(
    () => ({
      previous: sorted[indexCurrent + 1] || null,
      next: sorted[indexCurrent - 1] || null,
    }),
    [sorted, indexCurrent]
  );

  return data;
};
