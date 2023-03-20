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

import React, { memo } from 'react';
import { m } from 'framer-motion';
import { Box, ThemeUIStyleObject } from 'theme-ui';
import { themeRadii } from 'styles/theme/radii';
import {
  borderRadiusBottom,
  borderRadiusLeft,
  borderRadiusRight,
  borderRadiusTop,
} from 'styles/shortcuts';

const MotionTab = m(Box);

type Props = {
  layoutId: string;
  radiusOn: 'top' | 'right' | 'bottom' | 'left' | 'all';
};

export const SelectedTabIcon: React.FC<Props> = memo(({ layoutId, radiusOn }) => {
  const radius = themeRadii.medium;
  let borderRadius: ThemeUIStyleObject = { borderRadius: radius };
  if (radiusOn === 'top') borderRadius = borderRadiusTop(radius);
  if (radiusOn === 'right') borderRadius = borderRadiusRight(radius);
  if (radiusOn === 'bottom') borderRadius = borderRadiusBottom(radius);
  if (radiusOn === 'left') borderRadius = borderRadiusLeft(radius);
  return (
    <MotionTab
      initial={false}
      sx={{
        label: 'SelectedTabIcon',
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        height: '100%',
        background: 'redGradient',
        zIndex: 1,
        ...borderRadius,
      }}
      layoutId={layoutId}
    />
  );
});
