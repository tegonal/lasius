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
export const DataListRow: React.FC<Props> = ({ children }) => {
  return (
    <Box
      as="tr"
      sx={{
        label: 'DataListRow',
        borderBottom: '1px solid',
        borderBottomColor: 'containerTextColorMuted',
        '> td:first-of-type': { pl: 4 },
        '> td:last-of-type': { width: 48, pr: 2 },
      }}
    >
      {children}
    </Box>
  );
};
