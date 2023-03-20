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

import React, { memo } from 'react';

import { dateFormat } from 'projectConfig/dateFormat';
import { useRouter } from 'next/router';
import { IsoDateString } from 'lib/api/apiDateHandling';
import { isString } from 'lodash';
import { formatISOLocale } from 'lib/dates';

const dateFormats = {
  fullDateLong: 'PPpp',
  fullDateShort: 'P',
  year: 'yyyy',
  monthNameLong: 'MMMM',
  monthNameShort: 'MMM',
  day: 'd',
  dayPadded: 'dd',
  dayNameLong: 'eeee',
  dayNameShort: 'eeeeee',
  time: 'HH:mm',
};

export const FormatDate: React.FC<{
  date: IsoDateString | Date;
  format: keyof typeof dateFormats;
}> = memo(({ date, format }) => {
  const { locale } = useRouter();
  if (isString(date)) {
    return <>{dateFormat(date, dateFormats[format], locale)}</>;
  }
  return <>{dateFormat(formatISOLocale(date), dateFormats[format], locale)}</>;
});
