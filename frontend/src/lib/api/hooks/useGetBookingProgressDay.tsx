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

import { differenceInSeconds, isToday } from 'date-fns';
import { apiTimespanDay, IsoDateString } from 'lib/api/apiDateHandling';
import {
  useGetUserBookingCurrent,
  useGetUserBookingListByOrganisation,
} from 'lib/api/lasius/user-bookings/user-bookings';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useGetPlannedWorkingHoursByDate } from 'lib/api/hooks/useGetPlannedWorkingHoursByDate';
import { useInterval } from 'usehooks-ts';
import { UI_SLOW_DATA_DEDUPE_INTERVAL } from 'projectConfig/intervals';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { getExpectedVsBookedPercentage } from 'lib/api/functions/getExpectedVsBookedPercentage';
import { getModelsBookingSummary } from 'lib/api/functions/getModelsBookingSummary';

export const useGetBookingProgressDay = (date: IsoDateString) => {
  const { selectedOrganisationId } = useOrganisation();
  const { plannedHoursDay: plannedWorkingHours } = useGetPlannedWorkingHoursByDate(date);
  const day = useMemo(() => new Date(date), [date]);
  const [currentDuration, setCurrentDuration] = useState(0);

  const { data: bookings } = useGetUserBookingListByOrganisation(
    selectedOrganisationId,
    apiTimespanDay(date),
    {
      swr: {
        enabled: !!date,
        revalidateOnFocus: isToday(day),
        revalidateIfStale: isToday(day),
        dedupingInterval: isToday(day) ? 2000 : UI_SLOW_DATA_DEDUPE_INTERVAL,
      },
    }
  );

  const { data: currentBooking } = useGetUserBookingCurrent();

  const currentBookingDuration = useCallback(() => {
    if (isToday(day) && currentBooking?.booking?.start.dateTime) {
      setCurrentDuration(
        differenceInSeconds(new Date(), new Date(currentBooking?.booking?.start.dateTime)) / 60 / 60
      );
    } else {
      setCurrentDuration(0);
    }
  }, [currentBooking?.booking?.start.dateTime, day]);

  useEffect(() => {
    currentBookingDuration();
  }, [currentBookingDuration]);

  useInterval(currentBookingDuration, 1000);

  const summaryInit = useMemo(() => getModelsBookingSummary(bookings || []), [bookings]);

  const summary = { ...summaryInit, hours: summaryInit.hours + currentDuration };

  const { fulfilledPercentage, progressBarPercentage } = getExpectedVsBookedPercentage(
    plannedWorkingHours,
    summary.hours
  );

  return {
    ...summary,
    plannedWorkingHours,
    fulfilledPercentage,
    progressBarPercentage,
  };
};
