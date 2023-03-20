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
import { IsoDateString } from 'lib/api/apiDateHandling';
import { isWeekend } from 'date-fns';
import { Button } from '@theme-ui/components';
import { fullWidthHeight } from 'styles/shortcuts';
import { FormatDate } from 'components/shared/formatDate';

type Props = {
  date: IsoDateString;
  onClick: (args: any) => void;
};

export const CalendarDayCompact: React.FC<Props> = ({ date, onClick }) => {
  const day = new Date(date);
  const setDate = () => onClick(date);
  return (
    <Button
      variant="icon"
      sx={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        flexGrow: 1,
        textAlign: 'center',
        position: 'relative',
        zIndex: 2,
        ...fullWidthHeight(),
        opacity: isWeekend(day) ? 0.5 : 1,
        fontSize: 3,
        lineHeight: 1,
        p: 2,
      }}
      onClick={() => setDate()}
    >
      <FormatDate date={day} format="day" />
    </Button>
  );
};
