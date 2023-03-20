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

import { formatISOLocale } from 'lib/dates';
import {
  addMonths,
  addQuarters,
  addWeeks,
  addYears,
  endOfDay,
  endOfMonth,
  endOfQuarter,
  endOfWeek,
  endOfYear,
  startOfDay,
  startOfMonth,
  startOfQuarter,
  startOfWeek,
  startOfYear,
} from 'date-fns';

const t = (s: string) => s;

export const dateOptions = [
  {
    name: t('This week'),
    dateRangeFn: (day: Date) => {
      return {
        from: formatISOLocale(startOfWeek(day, { weekStartsOn: 1 })),
        to: formatISOLocale(endOfWeek(day, { weekStartsOn: 1 })),
      };
    },
  },
  {
    name: t('This month'),
    dateRangeFn: (day: Date) => {
      return { from: formatISOLocale(startOfMonth(day)), to: formatISOLocale(endOfMonth(day)) };
    },
  },
  {
    name: t('This quarter'),
    dateRangeFn: (day: Date) => {
      return { from: formatISOLocale(startOfQuarter(day)), to: formatISOLocale(endOfQuarter(day)) };
    },
  },
  {
    name: t('This year'),
    dateRangeFn: (day: Date) => {
      return { from: formatISOLocale(startOfYear(day)), to: formatISOLocale(endOfYear(day)) };
    },
  },
  {
    name: t('Last week'),
    dateRangeFn: (day: Date) => {
      const ref = addWeeks(day, -1);
      return {
        from: formatISOLocale(startOfWeek(ref, { weekStartsOn: 1 })),
        to: formatISOLocale(endOfWeek(ref, { weekStartsOn: 1 })),
      };
    },
  },
  {
    name: t('Last month'),
    dateRangeFn: (day: Date) => {
      const ref = addMonths(day, -1);
      return { from: formatISOLocale(startOfMonth(ref)), to: formatISOLocale(endOfMonth(ref)) };
    },
  },
  {
    name: t('Last quarter'),
    dateRangeFn: (day: Date) => {
      const ref = addQuarters(day, -1);
      return { from: formatISOLocale(startOfQuarter(ref)), to: formatISOLocale(endOfQuarter(ref)) };
    },
  },
  {
    name: t('Last year'),
    dateRangeFn: (day: Date) => {
      const ref = addYears(day, -1);
      return { from: formatISOLocale(startOfYear(ref)), to: formatISOLocale(endOfYear(ref)) };
    },
  },
  {
    name: t('Custom'),
    dateRangeFn: (day: Date) => {
      return { from: formatISOLocale(startOfDay(day)), to: formatISOLocale(endOfDay(day)) };
    },
  },
];
