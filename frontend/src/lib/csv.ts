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

import { ExportToCsv } from 'ts-export-to-csv';
import { modelsLocalDateTimeWithTimeZoneToString } from './api/apiDateHandling';
import { ExtendedHistoryBooking } from 'types/booking';

export const downloadCSVData = (csv: string, filename: string) => {
  const blob = new Blob([csv], {
    type: 'text/csv;charset=utf8;',
  });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.target = '_blank';
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.parentNode?.removeChild(link);
};

export const exportBookingListToCsv = (e: ExtendedHistoryBooking[], filename?: string) => {
  const data: Record<string, string | undefined>[] = e.map((item) => {
    return {
      user: item.userReference.key,
      organization: item.organisationReference.key,
      project: item.projectReference.key,
      tags: item.tags.map((tag) => tag.id).join(','),
      start: modelsLocalDateTimeWithTimeZoneToString(item.start),
      end: item.end ? modelsLocalDateTimeWithTimeZoneToString(item.end) : undefined,
      duration: item.duration.toString(),
      durationString: item.durationString,
    };
  });

  const csvExporter = new ExportToCsv({
    fieldSeparator: ',',
    quoteStrings: '"',
    decimalSeparator: '.',
    showLabels: true,
    showTitle: false,
    useTextFile: false,
    useBom: true,
    useKeysAsHeaders: true,
    filename: filename || 'lasius-bookings-export',
  });

  csvExporter.generateCsv(data);
};
