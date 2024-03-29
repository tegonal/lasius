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

import React, { useEffect, useState } from 'react';
import { Box, Flex } from 'theme-ui';
import { Icon } from 'components/shared/icon';
import { flexRowJustifyStartAlignCenter } from 'styles/shortcuts';
import { useInterval } from 'usehooks-ts';
import { durationAsString, formatISOLocale } from 'lib/dates';

type Props = { startDate: string };

export const BookingDurationCounter: React.FC<Props> = ({ startDate }) => {
  const [duration, setDuration] = useState<string>(`00:00`);

  useEffect(() => {
    setDuration(durationAsString(startDate, formatISOLocale(new Date())));
  }, [startDate]);

  useInterval(() => {
    setDuration(durationAsString(startDate, formatISOLocale(new Date())));
  }, 25000);

  return (
    <Flex sx={{ ...flexRowJustifyStartAlignCenter(1), lineHeight: 'normal' }}>
      <Icon name="stopwatch-interface-essential" size={14} />
      <Box>{duration}</Box>
    </Flex>
  );
};
