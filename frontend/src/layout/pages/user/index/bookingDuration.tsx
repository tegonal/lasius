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
import { Box, Flex } from 'theme-ui';
import { Icon } from 'components/shared/icon';
import { flexRowJustifyStartAlignCenter } from 'styles/shortcuts';
import { durationAsString } from 'lib/dates';
import { ModelsBooking } from 'lib/api/lasius';
import { ToolTip } from 'components/shared/toolTip';
import { useTranslation } from 'next-i18next';

type Props = { item: ModelsBooking };

export const BookingDuration: React.FC<Props> = ({ item }) => {
  const duration = durationAsString(item.start.dateTime, item.end?.dateTime || '');
  const durationIsZero = duration === '00:00';
  const { t } = useTranslation();
  return (
    <Flex
      sx={{
        ...flexRowJustifyStartAlignCenter(1),
        lineHeight: 'normal',
        ...(durationIsZero && { color: 'warning' }),
      }}
    >
      <Icon name="time-clock-three-interface-essential" size={14} />
      <Box>{duration}</Box>
      {durationIsZero && (
        <ToolTip toolTipContent={t("This booking's duration is zero")}>
          <Icon name="alert-triangle" size={14} />
        </ToolTip>
      )}
    </Flex>
  );
};
