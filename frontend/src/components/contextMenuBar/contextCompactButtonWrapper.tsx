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
import { Box } from 'theme-ui';

type Props = {
  children: React.ReactNode;
};
export const ContextCompactButtonWrapper: React.FC<Props> = ({ children }) => {
  return (
    <Box
      sx={{
        label: 'ContextCompactButtonWrapper',
        '&:last-child': {
          borderLeft: '1px solid',
          borderLeftColor: 'negativeTextMuted',
          px: 2,
        },
        px: 2,
        py: 2,
      }}
    >
      {children}
    </Box>
  );
};
