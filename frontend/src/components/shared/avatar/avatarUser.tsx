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
import Avatar from 'boring-avatars';
import { userAvatarPalette } from 'styles/theme/colors';
import { Strong } from 'components/tags/strong';
import { ToolTip } from 'components/shared/toolTip';

type Props = {
  firstName: string;
  lastName: string;
  size?: number;
};

export const AvatarUser: React.FC<Props> = ({ firstName, lastName, size = 39 }) => {
  return (
    <ToolTip offset={16} placement="right" toolTipContent={`${firstName} ${lastName}`}>
      <Box
        sx={{
          label: 'AvatarUser',
          position: 'relative',
          width: `${size}px`,
          height: `${size}px`,
          flexShrink: 0,
        }}
      >
        <Avatar
          square={false}
          size={size}
          name={`${firstName} ${lastName}`}
          variant="bauhaus"
          colors={userAvatarPalette}
        />
        <Flex
          sx={{
            label: 'AvatarUserName',
            flexShrink: 0,
            width: '100%',
            height: '100%',
            padding: 1,
            lineHeight: 'normal',
            fontSize: 1,
            color: 'negativeText',
            position: 'absolute',
            top: 0,
            left: 0,
            justifyContent: 'center',
            alignItems: 'flex-end',
            userSelect: 'none',
          }}
        >
          <Strong>
            {firstName[0].toUpperCase()}
            {lastName[0].toUpperCase()}
          </Strong>
        </Flex>
      </Box>
    </ToolTip>
  );
};
