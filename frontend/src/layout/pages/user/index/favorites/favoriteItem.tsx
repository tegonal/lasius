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
import { flexRowJustifyBetweenAlignCenter } from 'styles/shortcuts';
import { BookingName } from 'layout/pages/user/index/bookingName';
import { TagList } from 'components/shared/tagList';
import { FavoriteItemContext } from 'layout/pages/user/index/favorites/favoriteItemContext';
import { ModelsBookingStub } from 'lib/api/lasius';

type Props = {
  item: ModelsBookingStub;
};

export const FavoriteItem: React.FC<Props> = ({ item }) => {
  return (
    <Flex
      sx={{
        label: 'FavoriteItem',
        ...flexRowJustifyBetweenAlignCenter(2),
        px: 2,
        py: 2,
        borderBottom: '1px solid',
        borderBottomColor: 'containerTextColorMuted',
      }}
    >
      <Flex sx={{ flexDirection: 'column', gap: 1 }}>
        <BookingName item={item} />
        <TagList items={item.tags} />
      </Flex>
      <Box sx={{ flexShrink: 0 }}>
        <FavoriteItemContext item={item} />
      </Box>
    </Flex>
  );
};
