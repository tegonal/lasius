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

import { millisToHours } from 'lib/dates';
import { sortBy } from 'lodash';
import { ModelsBookingStats } from 'lib/api/lasius';

export const getTransformedChartDataAggregate = (
  data: ModelsBookingStats[] | undefined,
  limit = -1
) => {
  if (!data || data.length < 1 || data[0].values.length < 1) return { data: undefined };
  const keys = data[0].values.map((item) => item.label);
  const chartData = data[0].values
    .map((item) => ({
      id: item.label || '',
      value: millisToHours(item.duration || 0),
    }))
    .filter((item) => item.value > 0);

  const sortedData = sortBy(chartData, 'value');
  if (limit < 1 || limit > sortedData.length) {
    return { data: sortedData, keys };
  }
  const limitedData = sortedData.slice(sortedData.length - limit);
  return { data: limitedData, keys };
};
