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
import { useBreakpointIndex } from '@theme-ui/match-media';

type BreakpointsType = 'xs' | 'sm' | 'md' | 'lg';

type Props = {
  children: React.ReactNode;
  mode: 'show' | 'hide';
  on: BreakpointsType[];
};

/**
 * Renders or prevents rendering of children on defined media query definitions
 * @param children
 * @param mode
 * @param on
 */
export const Responsively: React.FC<Props> = ({ children, mode, on }) => {
  const bpidx = useBreakpointIndex({ defaultIndex: 0 });

  switch (true) {
    case bpidx === 0 && on.includes('xs'):
    case bpidx === 1 && on.includes('sm'):
    case bpidx === 2 && on.includes('md'):
    case bpidx >= 3 && on.includes('lg'):
      if (mode === 'show') {
        return <>{children}</>;
      }
      return null;
    default:
      return null;
  }
};
