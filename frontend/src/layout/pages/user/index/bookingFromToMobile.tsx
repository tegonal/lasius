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
import { Flex } from 'theme-ui';
import { FormatDate } from 'components/shared/formatDate';
import { ModelsBooking } from 'lib/api/lasius';
import { Icon } from 'components/shared/icon';

type Props = {
  item: ModelsBooking;
};

export const BookingFromToMobile: React.FC<Props> = ({ item }) => {
  const { start, end } = item;
  return (
    <Flex sx={{ lineHeight: 'normal', flexDirection: 'row', gap: 2 }}>
      <Flex sx={{ justifyContent: 'center', alignItems: 'center' }}>
        <FormatDate date={start.dateTime} format="time" />
      </Flex>
      <Flex sx={{ justifyContent: 'center', alignItems: 'center' }}>
        <Icon name="expand-horizontal-4" size={16} />
      </Flex>
      <Flex sx={{ justifyContent: 'center', alignItems: 'center' }}>
        <FormatDate date={end?.dateTime || ''} format="time" />
      </Flex>
    </Flex>
  );
};
