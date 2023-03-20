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

import { Granularity, NivoChartDataType } from 'types/common';
import { millisToHours } from 'lib/dates';
import { getCeilingValues } from 'lib/api/functions/getCeilingValues';
import { plannedWorkingHoursStub } from 'lib/stubPlannedWorkingHours';
import { ModelsBookingStats, ModelsBookingStatsCategory } from 'lib/api/lasius';

export const getNivoChartDataFromApiStatsData = (
  data: ModelsBookingStats[],
  granularity: Granularity,
  plannedWorkingHours: typeof plannedWorkingHoursStub
) => {
  if (data.length < 1) return undefined;
  const getCategoryLabel = (item: ModelsBookingStatsCategory) => {
    switch (granularity) {
      case 'Week':
        return `W ${item.week}`;
      case 'Day':
        return `${item.day}.${item.month}`;
      case 'Month':
        return `${item.month}/${String(item.year).slice(-2)}`;
      case 'Year':
        return String(item.year);
      default:
        return '';
    }
  };

  const chartData: NivoChartDataType = data.map((cat) => {
    const categoryLabel = getCategoryLabel(cat.category);
    const categoryValues = Object.fromEntries(
      cat.values.map((item) => [[item.label || ''], [millisToHours(item.duration || 0)]])
    );
    return {
      category: categoryLabel,
      ...categoryValues,
    };
  });

  const keys = [
    ...new Set(data.flatMap((category) => category.values.map((item) => item.label || ''))),
  ];

  const ceilingData: NivoChartDataType = data.map((cat) => {
    const categoryLabel = getCategoryLabel(cat.category);
    const ceilingDataValue = getCeilingValues(granularity, cat.category, plannedWorkingHours);
    return {
      category: categoryLabel,
      value: ceilingDataValue,
    };
  });

  return { data: chartData, keys, ceilingData };
};
