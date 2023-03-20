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

import React, { useEffect, useRef } from 'react';
import { clickableStyle, flexColumnJustifyCenterAlignCenter } from 'styles/shortcuts';
import { Box, Flex } from 'theme-ui';
import { themeRadii } from 'styles/theme/radii';
import { AnimateNumber } from 'components/shared/motion/animateNumber';
import { useBoolean } from 'usehooks-ts';
import { decimalHoursToObject } from 'lib/dates';

type Props = {
  value: number;
  label: string;
};

export const StatsTileHours: React.FC<Props> = ({ value, label }) => {
  const previousValue = useRef<number>(0);
  const previousHours = useRef<number>(0);
  const previousMinutes = useRef<number>(0);

  const showDecimalHours = useBoolean(false);

  useEffect(() => {
    const duration = decimalHoursToObject(value);
    previousValue.current = value;
    previousHours.current = duration.hours;
    previousMinutes.current = duration.minutes;
  }, [value]);

  return (
    <Flex
      sx={{
        label: 'StatCardHours',
        borderRadius: themeRadii.medium,
        lineHeight: 'normal',
        ...flexColumnJustifyCenterAlignCenter(2),
        textAlign: 'center',
        ...clickableStyle(),
        userSelect: 'none',
      }}
      onClick={showDecimalHours.toggle}
    >
      <Box sx={{ fontSize: 4 }}>
        {showDecimalHours.value ? (
          <AnimateNumber from={previousValue.current} to={value} />
        ) : (
          <>
            <AnimateNumber
              from={previousHours.current}
              to={decimalHoursToObject(value).hours}
              leftpad={1}
            />
            :
            <AnimateNumber
              from={previousMinutes.current}
              to={decimalHoursToObject(value).minutes}
              leftpad={1}
            />
          </>
        )}
      </Box>
      <Box sx={{ fontSize: 1 }}>{label}</Box>
    </Flex>
  );
};
