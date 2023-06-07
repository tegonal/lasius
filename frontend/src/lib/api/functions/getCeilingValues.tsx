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

import { formatISOLocale, getWorkingHoursWeekdayString } from 'lib/dates';
import { eachDayOfInterval, lastDayOfMonth, lastDayOfWeek } from 'date-fns';
import { plannedWorkingHoursStub } from 'lib/stubPlannedWorkingHours';
import { getPlannedWorkingHoursForDateRange } from 'lib/api/functions/getPlannedWorkingHoursForDateRange';
import { ModelsBookingStatsCategory } from 'lib/api/lasius';
import { Granularity } from 'types/common';

// Returns the first day (Monday) of the specified week
// Year defaults to the current local calendar year
function getISOWeek(w: number, y: number = new Date().getFullYear()) {
  const d = new Date(y, 0, 4);
  d.setDate(d.getDate() - (d.getDay() || 7) + 1 + 7 * (w - 1));
  return d;
}

export const getCeilingValues = (
  granularity: Granularity,
  item: ModelsBookingStatsCategory,
  plannedWorkingHours: typeof plannedWorkingHoursStub
) => {
  switch (granularity) {
    case 'Day': {
      const weekday = getWorkingHoursWeekdayString(
        formatISOLocale(new Date(item.year as number, item.month as number, item.day as number))
      );
      return plannedWorkingHours[weekday];
    }
    case 'Week': {
      const dateOfWeek = getISOWeek(item.week || 0, item.year || undefined);
      const datesInMonth = eachDayOfInterval({
        start: dateOfWeek,
        end: lastDayOfWeek(dateOfWeek),
      });
      return getPlannedWorkingHoursForDateRange(datesInMonth, plannedWorkingHours);
    }
    case 'Month': {
      const dateOfMonth = new Date(`${item.year}-${item.month}-01`);
      const datesInMonth = eachDayOfInterval({
        start: dateOfMonth,
        end: lastDayOfMonth(dateOfMonth),
      });
      return getPlannedWorkingHoursForDateRange(datesInMonth, plannedWorkingHours);
    }
    case 'Year': {
      const datesInYear = eachDayOfInterval({
        start: new Date(`${item.year}-01-01`),
        end: new Date(`${item.year}-12-31`),
      });
      return getPlannedWorkingHoursForDateRange(datesInYear, plannedWorkingHours);
    }
    default:
      return 0;
  }
};
