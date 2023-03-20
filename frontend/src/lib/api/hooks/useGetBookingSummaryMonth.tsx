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

import { isToday } from 'date-fns';
import { apiTimespanMonth, IsoDateString } from 'lib/api/apiDateHandling';
import { useGetUserBookingListByOrganisation } from 'lib/api/lasius/user-bookings/user-bookings';
import { useMemo } from 'react';
import { UI_SLOW_DATA_DEDUPE_INTERVAL } from 'projectConfig/intervals';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { getModelsBookingSummary } from 'lib/api/functions/getModelsBookingSummary';

export const useGetBookingSummaryMonth = (date: IsoDateString) => {
  const { selectedOrganisationId } = useOrganisation();
  const day = new Date(date);

  const { data: bookings } = useGetUserBookingListByOrganisation(
    selectedOrganisationId,
    apiTimespanMonth(date),
    {
      swr: {
        enabled: !!date,
        revalidateOnFocus: isToday(day),
        revalidateIfStale: isToday(day),
        dedupingInterval: isToday(day) ? 2000 : UI_SLOW_DATA_DEDUPE_INTERVAL,
      },
    }
  );

  const summary = useMemo(() => getModelsBookingSummary(bookings || []), [bookings]);

  return {
    ...summary,
  };
};
