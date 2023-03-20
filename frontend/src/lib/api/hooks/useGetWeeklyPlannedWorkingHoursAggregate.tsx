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
import { UI_SLOW_DATA_DEDUPE_INTERVAL } from 'projectConfig/intervals';
import { useMemo } from 'react';
import { ModelsWorkingHours } from 'lib/api/lasius';

export const useGetWeeklyPlannedWorkingHoursAggregate = () => {
  const { data } = useGetUserProfile({
    swr: {
      revalidateOnFocus: false,
      dedupingInterval: UI_SLOW_DATA_DEDUPE_INTERVAL,
    },
  });

  const allOrganisationsWorkingHours = useMemo(() => {
    const allOrganisations: ModelsWorkingHours = { ...plannedWorkingHoursStub };
    data?.organisations.forEach((item) => {
      const { plannedWorkingHours } = item;
      if (plannedWorkingHours) {
        Object.keys(plannedWorkingHours).forEach((day: any) => {
          // eslint-disable-next-line @typescript-eslint/ban-ts-comment
          // @ts-ignore
          allOrganisations[day] += plannedWorkingHours[day];
        });
      }
    });
    return allOrganisations;
  }, [data]);

  const selectedOrganisationWorkingHours = useMemo(() => {
    if (data && data.organisations && data.organisations.length > 0) {
      const workingHours = data.organisations.find(
        (org) => org.organisationReference.id === data?.settings?.lastSelectedOrganisation?.id
      );
      return workingHours ? workingHours.plannedWorkingHours : plannedWorkingHoursStub;
    }
    return plannedWorkingHoursStub;
  }, [data]);

  const selectedOrganisationWorkingHoursTotal = useMemo(
    () =>
      Object.entries(selectedOrganisationWorkingHours).reduce((a, c) => ['total', a[1] + c[1]])[1],
    [selectedOrganisationWorkingHours]
  );

  return {
    allOrganisationsWorkingHours,
    selectedOrganisationWorkingHours,
    selectedOrganisationWorkingHoursTotal,
  };
};
