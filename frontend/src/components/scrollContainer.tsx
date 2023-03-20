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
import { Box, ThemeUIStyleObject } from 'theme-ui';
import { scrollbarStyle } from 'styles/shortcuts';
import { noop } from 'lodash';

type Props = {
  children: React.ReactNode;
  sx?: ThemeUIStyleObject;
  onScroll?: (event: React.UIEvent<HTMLDivElement, UIEvent>) => void;
};

/**
 * A container that can be scrolled vertically. Must be part of a grid.
 * @param children
 * @param sx
 */
export const ScrollContainer: React.FC<Props> = ({ children, sx = {}, onScroll = noop }) => {
  return (
    <Box
      sx={{
        label: 'ScrollContainer',
        height: '100%',
        overflow: 'auto',
        overflowY: 'auto',
        overflowX: 'hidden',
        position: 'relative',
        overflowScrolling: 'touch',
        scrollBehavior: 'smooth',
        ...scrollbarStyle(),
        ...sx,
      }}
      onScroll={onScroll}
    >
      {children}
    </Box>
  );
};
