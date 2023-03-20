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

import { round } from 'lodash';

export const getExpectedVsBookedPercentage = (expected: number, worked: number) => {
  let fulfilledPercentage = 0;
  if (worked === 0) fulfilledPercentage = 0;
  if (expected === 0 && worked > 0) fulfilledPercentage = 100;
  if (expected > 0 && worked > 0) fulfilledPercentage = round((worked / expected) * 100, 2);

  const progressBarPercentage =
    fulfilledPercentage > 90 && fulfilledPercentage < 100
      ? 90
      : fulfilledPercentage > 100
      ? 100
      : fulfilledPercentage;

  return {
    fulfilledPercentage,
    progressBarPercentage,
  };
};
