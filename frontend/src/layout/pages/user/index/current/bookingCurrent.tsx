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
import { BookingCurrentEntry } from 'layout/pages/user/index/current/bookingCurrentEntry';
import { flexRowJustifyStartAlignCenter, fullWidthHeight } from 'styles/shortcuts';

type Props = {
  inContainer?: boolean;
};

export const BookingCurrent: React.FC<Props> = ({ inContainer = true }) => {
  return (
    <Flex
      sx={{
        label: 'BookingCurrent',
        boxSizing: 'border-box',
        width: '100%',
        minHeight: 96,
        height: '100%',
        px: [2, 3, 4],
        py: 3,
        ...flexRowJustifyStartAlignCenter(3),
        overflow: 'hidden',
      }}
    >
      <Box sx={{ position: 'relative', ...fullWidthHeight() }}>
        <BookingCurrentEntry inContainer={inContainer} />
      </Box>
    </Flex>
  );
};
