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

import React from 'react';
import { FormElement } from 'components/forms/formElement';
import { FormGroup } from 'components/forms/formGroup';
import { WorkingHoursWeek } from 'layout/pages/user/workingHours/week/workingHoursWeek';
import { Box } from 'theme-ui';
import { WorkingHoursSummary } from 'layout/pages/user/workingHours/week/workingHoursSummary';
import { useGetWeeklyPlannedWorkingHoursAggregate } from 'lib/api/hooks/useGetWeeklyPlannedWorkingHoursAggregate';
import { useIsClient } from 'usehooks-ts';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';

export const WorkingHoursForm: React.FC = () => {
  const { organisations } = useOrganisation();
  const { allOrganisationsWorkingHours } = useGetWeeklyPlannedWorkingHoursAggregate();
  const isClient = useIsClient();

  if (!isClient) return null;

  return (
    <>
      <Box sx={{ mb: 3, width: '100%' }}>
        <FormGroup>
          <FormElement>
            <WorkingHoursSummary aggregatedPlannedWorkingHours={allOrganisationsWorkingHours} />
          </FormElement>
        </FormGroup>
      </Box>
      {organisations?.map((org) => (
        <Box key={org.organisationReference.id} sx={{ mb: 3, width: '100%' }}>
          <FormGroup>
            <FormElement>
              <WorkingHoursWeek organisation={org} />
            </FormElement>
          </FormGroup>
        </Box>
      ))}
    </>
  );
};
