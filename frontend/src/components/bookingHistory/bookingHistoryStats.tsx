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

type Props = {
  hours: number;
  bookings: number;
};

export const BookingHistoryStats: React.FC<Props> = ({ hours, bookings }) => {
  const { t } = useTranslation('common');
  return (
    <Box sx={{ width: '100%' }}>
      <Heading variant="headingUnderlinedMuted">{t('Current selection')}</Heading>
      <Grid sx={{ gap: 3, gridTemplateColumns: '1fr 1fr', width: '100%', pb: 2 }}>
        <StatsTileHours value={hours} label={t('Hours')} />
        <StatsTileNumber value={bookings} label={t('Bookings')} />
      </Grid>
    </Box>
  );
};
