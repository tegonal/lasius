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
import { m } from 'framer-motion';
import { Box } from 'theme-ui';

const MotionTab = m(Box);

type Props = {
  layoutId: string;
};

export const SelectedTab: React.FC<Props> = ({ layoutId }) => {
  return (
    <MotionTab
      initial={false}
      sx={{
        label: 'SelectedTab',
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        height: 2,
        background: 'redGradient',
        zIndex: 1,
      }}
      layoutId={layoutId}
    />
  );
};
