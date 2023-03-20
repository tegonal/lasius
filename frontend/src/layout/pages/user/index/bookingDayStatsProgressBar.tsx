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

import React, { memo, useEffect } from 'react';
import { Box } from 'theme-ui';
import { useGetBookingProgressDay } from 'lib/api/hooks/useGetBookingProgressDay';
import { ToolTip } from 'components/shared/toolTip';
import { decimalHoursToDurationString } from 'lib/dates';
import { useTranslation } from 'next-i18next';
import { useIsClient } from 'usehooks-ts';
import { useStore } from 'storeContext/store';

const ProgressBar: React.FC<{ percentage: number; label: string }> = memo(
  ({ percentage, label }) => {
    return (
      <Box
        sx={{
          width: '100%',
        }}
      >
        <ToolTip toolTipContent={label}>
          <Box
            sx={{
              width: '100%',
              height: 6,
              background: 'containerBackgroundLighter',
              fontSize: '10px',
              borderRadius: 4,
              overflow: 'hidden',
            }}
          >
            <Box
              sx={{
                width: `${percentage}%`,
                transition: 'width 1s ease',
                maxWidth: '100%',
                height: '100%',
                background: 'greenGradient',
              }}
            />
          </Box>
        </ToolTip>
      </Box>
    );
  }
);

export const BookingDayStatsProgressBar: React.FC = () => {
  const {
    state: { calendar },
  } = useStore();
  const day = useGetBookingProgressDay(calendar.selectedDate);
  const { t } = useTranslation('common');
  const [label, setLabel] = React.useState('');
  const isClient = useIsClient();

  useEffect(() => {
    if (day) {
      setLabel(
        `${day.fulfilledPercentage}% (${decimalHoursToDurationString(day.hours)} ${t(
          'of'
        )} ${decimalHoursToDurationString(day.plannedWorkingHours)})`
      );
    }
  }, [day, t]);

  if (!isClient) return null;

  return (
    <Box sx={{ width: '100%', px: 2 }}>
      <ProgressBar percentage={day.progressBarPercentage} label={label} />
    </Box>
  );
};
