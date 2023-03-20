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

import { Button } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import React from 'react';
import { ThemeUIStyleObject } from 'theme-ui';

type Props = {
  onClick: () => void;
  sx?: ThemeUIStyleObject;
};

export const ButtonCalendar: React.FC<Props> = ({ onClick }) => {
  return (
    <Button
      type="button"
      variant="iconMuted"
      tabIndex={-1}
      onClick={onClick}
      sx={{
        padding: 0,
        width: 'auto',
        outline: 'none',
        color: 'containerTextColor',
      }}
    >
      <Icon name="calendar-1-interface-essential" size={24} />
    </Button>
  );
};
