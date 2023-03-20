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
import { Box, Flex } from 'theme-ui';
import { IsoDateString } from 'lib/api/apiDateHandling';
import { isToday, isWeekend } from 'date-fns';
import { Button } from '@theme-ui/components';
import { DotRed } from 'components/shared/dots/dotRed';
import { ProgressSmall } from 'components/shared/progressSmall';
import { FormatDate } from 'components/shared/formatDate';
import { useGetBookingSummaryDay } from 'lib/api/hooks/useGetBookingSummaryDay';
import { useIsClient } from 'usehooks-ts';

type Props = {
  date: IsoDateString;
  onClick: (args: any) => void;
};

export const CalendarDay: React.FC<Props> = ({ date, onClick }) => {
  const isClient = useIsClient();
  const day = new Date(date);

  const { progressBarPercentage } = useGetBookingSummaryDay(date);

  if (!isClient) return null;

  const handleDayClick = () => onClick(date);

  return (
    <Button
      variant="icon"
      sx={{
        label: 'CalendarDay',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'flex-start',
        alignItems: 'center',
        flexGrow: 1,
        textAlign: 'center',
        position: 'relative',
        zIndex: 2,
        width: '100%',
        opacity: isWeekend(day) ? 0.5 : 1,
        minHeight: 78,
        minWidth: 56,
      }}
      onClick={handleDayClick}
    >
      <Box
        sx={{
          fontSize: 0,
          fontWeight: 500,
          lineHeight: 'normal',
          pt: 1,
          textTransform: 'uppercase',
          textAlign: 'center',
        }}
      >
        <FormatDate date={day} format="dayNameShort" />
      </Box>
      <Box sx={{ fontSize: 4, lineHeight: 'normal', pb: 2 }}>
        <FormatDate date={day} format="dayPadded" />{' '}
      </Box>
      {progressBarPercentage > 0 && (
        <Flex sx={{ justifyContent: 'center', width: '100%', pb: 2, px: 2 }}>
          <ProgressSmall percentage={progressBarPercentage} />
        </Flex>
      )}
      {isToday(day) && (
        <Flex sx={{ justifyContent: 'center', width: '100%', pb: 1 }}>
          <DotRed />
        </Flex>
      )}
    </Button>
  );
};
