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
import { Button } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import { IconNames } from 'types/iconNames';

type Props = {
  onClick: React.MouseEventHandler<HTMLButtonElement>;
  sx?: ThemeUIStyleObject;
  direction: 'up' | 'down';
};

export const ButtonUpDown: React.FC<Props> = ({ onClick, sx, direction }) => {
  const iconNames: Record<typeof direction, IconNames> = {
    up: 'arrow-up-1-arrows-diagrams',
    down: 'arrow-down-1-arrows-diagrams',
  };
  return (
    <Box sx={{ ...{ width: '100%' }, ...sx }}>
      {/* render as Box and no tabindex to not activate this button with react useTimeField, useDateField */}
      <Button
        type="button"
        variant="iconMuted"
        onClick={onClick}
        sx={{ py: '5px', px: 0 }}
        as={Box}
      >
        <Icon name={iconNames[direction]} size={16} />
      </Button>
    </Box>
  );
};
