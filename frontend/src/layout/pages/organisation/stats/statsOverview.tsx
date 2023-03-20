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

import React, { useMemo } from 'react';
import { Box, Grid, Heading } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { StatsTileNumber } from 'components/shared/statsTileNumber';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useFormContext } from 'react-hook-form';
import { apiTimespanFromTo } from 'lib/api/apiDateHandling';
import { getModelsBookingSummary } from 'lib/api/functions/getModelsBookingSummary';
import { statsSwrConfig } from 'components/shared/stats/statsSwrConfig';
import { useGetOrganisationBookingList } from 'lib/api/lasius/organisation-bookings/organisation-bookings';

export const StatsOverview: React.FC = () => {
  const { t } = useTranslation('common');
  const { selectedOrganisationId } = useOrganisation();
  const parentFormContext = useFormContext();

  const { data, isValidating } = useGetOrganisationBookingList(
    selectedOrganisationId,
    {
      ...apiTimespanFromTo(parentFormContext.watch('from'), parentFormContext.watch('to')),
    },
    statsSwrConfig
  );

  const summary = useMemo(
    () => (data && !isValidating ? getModelsBookingSummary(data) : undefined),
    [data, isValidating]
  );

  return (
    <Box sx={{ width: '100%' }}>
      <Heading variant="headingUnderlinedMuted">{t('Summary')}</Heading>
      <Grid sx={{ gap: 4, gridTemplateColumns: 'repeat(2, 1fr)', width: '100%', pb: 4 }}>
        <StatsTileNumber value={summary?.hours || 0} label={t('Hours')} />
        <StatsTileNumber value={summary?.elements || 0} label={t('Bookings')} />
      </Grid>
    </Box>
  );
};
