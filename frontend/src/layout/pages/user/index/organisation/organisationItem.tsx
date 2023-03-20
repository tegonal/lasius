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
import {
  flexRowJustifyBetweenAlignCenter,
  flexRowJustifyCenterAlignCenter,
} from 'styles/shortcuts';
import { BookingName } from 'layout/pages/user/index/bookingName';
import { TagList } from 'components/shared/tagList';
import { OrganisationItemContext } from 'layout/pages/user/index/organisation/organisationItemContext';
import { AvatarUser } from 'components/shared/avatar/avatarUser';
import { ModelsCurrentUserTimeBooking } from 'lib/api/lasius';

type Props = {
  item: ModelsCurrentUserTimeBooking;
};

export const OrganisationItem: React.FC<Props> = ({ item }) => {
  if (!item?.userReference?.key) return null;
  const user = item.userReference.key;
  const firstName = user.split('.')[0] || user[0];
  const lastName = user.split('.')[1] || user[1];
  const { booking } = item;
  return (
    <Flex
      sx={{
        label: 'OrganisationItem',
        ...flexRowJustifyBetweenAlignCenter(2),
        pr: 2,
        py: 2,
        borderBottom: '1px solid',
        borderBottomColor: 'containerTextColorMuted',
        overflowX: 'hidden',
        overflowY: 'hidden',
      }}
    >
      <Flex
        sx={{
          ...flexRowJustifyCenterAlignCenter(2),
          ...(!booking ? { filter: 'grayscale(20)', opacity: 0.333 } : {}),
        }}
      >
        <AvatarUser firstName={firstName} lastName={lastName} />
        {booking && (
          <Flex sx={{ flexDirection: 'column', gap: 1 }}>
            <BookingName variant="compact" item={booking} />
            <TagList items={booking.tags} />
          </Flex>
        )}
      </Flex>
      {booking && (
        <Box sx={{ flexShrink: 0 }}>
          <OrganisationItemContext item={item} />
        </Box>
      )}
    </Flex>
  );
};
