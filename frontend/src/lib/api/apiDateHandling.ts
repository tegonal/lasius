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

import {
  differenceInCalendarDays,
  endOfDay,
  endOfMonth,
  endOfWeek,
  format,
  parseISO,
  startOfDay,
  startOfMonth,
  startOfWeek,
} from 'date-fns';
import { utcToZonedTime, zonedTimeToUtc } from 'date-fns-tz';
import { ModelsLocalDateTimeWithTimeZone } from 'lib/api/lasius';
import { Granularity } from 'types/common';

export type ApiDateParam = string;
export type IsoDateString = string;
export const apiUrlDateParamFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";
export const apiUrlDateFormat = 'yyyy-MM-dd';

export const formatDateTimeToURLParam = (date: Date) => {
  return format(date, apiUrlDateParamFormat);
};

export const formatDateToURLParam = (date: Date) => {
  return format(date, apiUrlDateFormat);
};

export const apiTimespanFromTo = (
  from: IsoDateString,
  to: IsoDateString
): { from: ApiDateParam; to: ApiDateParam } => {
  return {
    from: formatDateTimeToURLParam(startOfDay(new Date(from))),
    to: formatDateTimeToURLParam(endOfDay(new Date(to))),
  };
};

export const apiDatespanFromTo = (
  from: IsoDateString,
  to: IsoDateString
): { from: ApiDateParam; to: ApiDateParam } => {
  return {
    from: formatDateToURLParam(startOfDay(new Date(from))),
    to: formatDateToURLParam(endOfDay(new Date(to))),
  };
};

export const granularityFromDatespanFromTo = (
  from: IsoDateString,
  to: IsoDateString
): Granularity => {
  const days = differenceInCalendarDays(new Date(to), new Date(from));
  if (!days) {
    return 'Day';
  }
  if (days > 400) {
    return 'Year';
  }
  if (days > 95) {
    return 'Month';
  }
  if (days > 15) {
    return 'Week';
  }
  return 'Day';
};

/**
 * Get from and to date based on a given day, spanning the entire day
 * @param date
 */
export const apiTimespanDay = (date: IsoDateString): { from: ApiDateParam; to: ApiDateParam } => {
  const dateObj = new Date(date);
  return {
    from: formatDateTimeToURLParam(startOfDay(dateObj)),
    to: formatDateTimeToURLParam(endOfDay(dateObj)),
  };
};

/**
 * Get from and to date based on a given day, spanning the entire week this day is part of
 * @param date
 */
export const apiTimespanWeek = (date: IsoDateString): { from: ApiDateParam; to: ApiDateParam } => {
  const dateObj = new Date(date);
  return {
    from: formatDateTimeToURLParam(startOfDay(startOfWeek(dateObj, { weekStartsOn: 1 }))),
    to: formatDateTimeToURLParam(endOfDay(endOfWeek(dateObj, { weekStartsOn: 1 }))),
  };
};

/**
 * Get from and to date based on a given day, spanning the entire month this day is part of
 * @param date
 */
export const apiTimespanMonth = (date: IsoDateString): { from: ApiDateParam; to: ApiDateParam } => {
  const dateObj = new Date(date);
  return {
    from: formatDateTimeToURLParam(startOfDay(startOfMonth(dateObj))),
    to: formatDateTimeToURLParam(endOfDay(endOfMonth(dateObj))),
  };
};

export const modelsLocalDateTimeWithTimeZoneToString = (
  m: ModelsLocalDateTimeWithTimeZone
): string => {
  const isoDate = parseISO(m.dateTime);
  const asUtc = zonedTimeToUtc(isoDate, m.zone);
  const zoned = utcToZonedTime(asUtc, m.zone);
  return zoned.toISOString();
};
