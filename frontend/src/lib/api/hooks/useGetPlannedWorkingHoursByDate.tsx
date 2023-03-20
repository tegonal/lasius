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

import { useGetUserProfile } from 'lib/api/lasius/user/user';
import { plannedWorkingHoursStub } from 'lib/stubPlannedWorkingHours';
import { getWorkingHoursWeekdayString } from 'lib/dates';
import { UI_SLOW_DATA_DEDUPE_INTERVAL } from 'projectConfig/intervals';

export const useGetPlannedWorkingHoursByDate = (date: string) => {
  const { data } = useGetUserProfile({
    swr: {
      enabled: !!date,
      revalidateOnFocus: false,
      dedupingInterval: UI_SLOW_DATA_DEDUPE_INTERVAL,
    },
  });

  const lastSelectedOrganisationId =
    data?.settings.lastSelectedOrganisation?.id ||
    data?.organisations.filter((item) => item.private)[0].organisationReference.id;

  const week = data?.organisations.filter(
    (org) => org.organisationReference.id === lastSelectedOrganisationId
  )[0].plannedWorkingHours;

  const plannedHoursDay = { ...plannedWorkingHoursStub, ...week }[
    getWorkingHoursWeekdayString(date)
  ];

  const allOrganisationsByDay = { ...plannedWorkingHoursStub };
  data?.organisations.forEach((item) => {
    const { plannedWorkingHours } = item;
    if (plannedWorkingHours) {
      Object.keys(plannedWorkingHours).forEach((day) => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        allOrganisationsByDay[day] += plannedWorkingHours[day];
      });
    }
  });

  const plannedHoursWeek = Object.entries({ ...plannedWorkingHoursStub, ...week }).reduce(
    (acc, [_key, value]) => ['total', acc[1] + value]
  )[1];

  return { plannedHoursWeek, plannedHoursDay, allOrganisationsByDay };
};
