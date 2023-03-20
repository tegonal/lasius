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
import { Button } from '@theme-ui/components';
import { flexColumnJustifyCenterAlignCenter } from 'styles/shortcuts';
import { transparentize } from '@theme-ui/color';
import { themeRadii } from 'styles/theme/radii';

type Props = {
  children: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
  borderRadius?: string;
};
export const CardSmall: React.FC<Props> = ({
  children,
  onClick,
  disabled = false,
  borderRadius = themeRadii.medium,
}) => {
  return (
    <Button
      variant="icon"
      sx={{
        label: 'CardSmall',
        display: 'flex',
        position: 'relative',
        background: transparentize('containerTextColor', 0.95),
        width: '100%',
        borderRadius,
        px: 3,
        pt: 3,
        pb: 3,
        ...flexColumnJustifyCenterAlignCenter(3),
        ...(!disabled && {
          '&:hover': {
            background: transparentize('containerTextColor', 0.89),
          },
        }),
      }}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </Button>
  );
};
