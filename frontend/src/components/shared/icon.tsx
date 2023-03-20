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
import { Box } from '@theme-ui/components';
import { IconNames } from 'types/iconNames';

export const Icon: React.FC<{
  name: IconNames;
  size: number;
  color?: string | undefined;
  alt?: string;
}> = memo(({ size, name, color = '', alt = '' }) => {
  return (
    <Box
      sx={{
        label: 'Icon',
        width: size,
        height: size,
        display: 'block',
        color: 'inherit',
        fill: color || 'transparent',
        stroke: color || 'currentColor',
        pointerEvents: 'none',
        userSelect: 'none',
        WebkitTouchCallout: 'none',
        WebkitTapHighlightColor: 'transparent',
        transition: 'fill 0.15s, stroke 0.15s',
      }}
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      alt={alt}
      as="svg"
    >
      <use xlinkHref={`/symbols.svg#${name}`} />
    </Box>
  );
});
