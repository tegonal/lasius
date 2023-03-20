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
import {
  flexColumnJustifyCenterAlignEnd,
  flexRowJustifyBetweenAlignCenter,
  flexRowJustifyStartAlignCenter,
} from 'styles/shortcuts';
import { BookingName } from 'layout/pages/user/index/bookingName';
import { BookingFromTo } from 'layout/pages/user/index/bookingFromTo';
import { TagList } from 'components/shared/tagList';
import { BookingDuration } from 'layout/pages/user/index/bookingDuration';
import { BookingItemContext } from 'layout/pages/user/index/list/bookingItemContext';
import { Responsively } from 'components/shared/responsively';
import { ModelsBooking } from 'lib/api/lasius';

type Props = {
  item: ModelsBooking;
};

export const BookingItem: React.FC<Props> = ({ item }) => {
  return (
    <Flex
      sx={{
        label: 'BookingItem',
        ...flexRowJustifyBetweenAlignCenter(2),
        px: [2, 2, 4],
        py: [3, 3, 4],
        borderBottom: '1px solid',
        borderBottomColor: 'containerTextColorMuted',
      }}
    >
      <Flex sx={{ flexDirection: 'column', gap: 1 }}>
        <BookingName item={item} />
        <TagList items={item.tags} />
      </Flex>
      <Flex sx={{ ...flexRowJustifyStartAlignCenter([2, 2, 4]), height: '100%', flexShrink: 0 }}>
        <Responsively mode="show" on={['md', 'lg']}>
          <Flex sx={{ ...flexRowJustifyStartAlignCenter([2, 2, 4]), height: '100%' }}>
            <BookingFromTo item={item} />
            <BookingDuration item={item} />
          </Flex>
        </Responsively>
        <Responsively mode="show" on={['xs', 'sm']}>
          <Flex sx={{ ...flexColumnJustifyCenterAlignEnd(1), height: '100%' }}>
            <BookingFromTo item={item} />
            <BookingDuration item={item} />
          </Flex>
        </Responsively>
        <BookingItemContext item={item} />
      </Flex>
    </Flex>
  );
};
