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
import { Box, Grid, Heading } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { StatsTileNumber } from 'components/shared/statsTileNumber';
import { StatsTileHours } from 'components/shared/statsTileHours';
import { useGetBookingSummaryDay } from 'lib/api/hooks/useGetBookingSummaryDay';
import { useGetBookingSummaryWeek } from 'lib/api/hooks/useGetbookingSummaryWeek';
import { useGetBookingSummaryMonth } from 'lib/api/hooks/useGetBookingSummaryMonth';
import { useStore } from 'storeContext/store';

export const ThisMonthStats: React.FC = () => {
  const { t } = useTranslation('common');
  const {
    state: { calendar },
  } = useStore();

  const day = useGetBookingSummaryDay(calendar.selectedDate);
  const week = useGetBookingSummaryWeek(calendar.selectedDate);
  const month = useGetBookingSummaryMonth(calendar.selectedDate);

  return (
    <Box sx={{ width: '100%' }}>
      <Heading variant="headingUnderlinedMuted">{t('Selected month')}</Heading>
      <Grid sx={{ gap: 3, gridTemplateColumns: '1fr 1fr', width: '100%', pb: 4 }}>
        <StatsTileHours value={month.hours} label={t('Hours')} />
        <StatsTileNumber value={month.elements} label={t('Bookings')} />
      </Grid>
      <Heading variant="headingUnderlinedMuted">{t('Selected week')}</Heading>
      <Grid sx={{ gap: 3, gridTemplateColumns: '1fr 1fr', width: '100%', pb: 4 }}>
        <StatsTileHours value={week.hours} label={t('Hours')} />
        <StatsTileNumber value={week.elements} label={t('Bookings')} />
        <StatsTileHours value={week.plannedWorkingHours} label={t('Expected hours')} />
        <StatsTileNumber value={week.fulfilledPercentage} label={t('% of expected')} />
      </Grid>
      <Heading variant="headingUnderlinedMuted">{t('Selected day')}</Heading>
      <Grid sx={{ gap: 3, gridTemplateColumns: '1fr 1fr', width: '100%', pb: 4 }}>
        <StatsTileHours value={day.hours} label={t('Hours')} />
        <StatsTileNumber value={day.elements} label={t('Bookings')} />
        <StatsTileHours value={day.plannedWorkingHours} label={t('Expected hours')} />
        <StatsTileNumber value={day.fulfilledPercentage} label={t('% of expected')} />
      </Grid>
    </Box>
  );
};
