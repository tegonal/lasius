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
import { Box, ThemeUIStyleObject, useColorMode } from 'theme-ui';
import { themeRadii } from 'styles/theme/radii';
import { effects } from 'styles/theme/effects';
import { scrollbarStyle } from 'styles/shortcuts';

type Props = {
  children: React.ReactNode;
  sx?: ThemeUIStyleObject;
};

export const DropdownList: React.FC<Props> = ({ children, sx = {} }) => {
  const [colorMode] = useColorMode();
  return (
    <Box
      sx={{
        position: 'absolute',
        background: 'containerBackground',
        borderBottomLeftRadius: themeRadii.small,
        borderBottomRightRadius: themeRadii.small,
        borderTop: '1px solid',
        borderTopColor: 'containerBackground',
        py: 3,
        width: '100%',
        height: 'auto',
        overflow: 'auto',
        maxHeight: '200px',
        ...scrollbarStyle(),
        ...(colorMode === 'dark'
          ? effects.shadows.softShadowOnDark
          : effects.shadows.softShadowOnWhiteSmall),
        zIndex: 1,
        ...sx,
      }}
    >
      {children}
    </Box>
  );
};
