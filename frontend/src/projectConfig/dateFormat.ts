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

import { format, formatRelative, Locale } from 'date-fns';
import { de } from 'date-fns/locale';

// This needs to be manually matched to ./api/dotenv.ts
const locales: { [key: string]: Locale } = { de };

export const dateFormat = (
  dateString: string,
  formatStr: any = 'PP',
  locale: string | number = 'de'
): string => {
  const date = new Date(dateString);
  return format(date, formatStr, {
    locale: locales[locale],
  });
};

export const dateFormatRelative = (dateString: string, locale = 'de'): string => {
  const date = new Date(dateString);
  return formatRelative(date, new Date(), {
    locale: locales[locale],
  });
};

export const TIME_FORMAT = 'HH:mm';
