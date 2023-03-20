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
import { useTranslation } from 'next-i18next';
import { Icon } from 'components/shared/icon';
import { clickableStyle, flexRowJustifyCenterAlignCenter, fullWidthHeight } from 'styles/shortcuts';
import { useRouter } from 'next/router';

export const BookingCurrentNoBooking: React.FC = () => {
  const { t } = useTranslation('common');
  const { pathname = '', push } = useRouter();

  if (pathname !== '/user/home' && pathname !== '/') {
    return (
      <Flex
        onClick={() => push('/')}
        sx={{ ...fullWidthHeight(), ...flexRowJustifyCenterAlignCenter(3), ...clickableStyle() }}
      >
        <Box>
          <Icon name="time-clock-circle-interface-essential" size={24} />
        </Box>
        <Box>{t('Currently not booking')}</Box>
      </Flex>
    );
  }

  return (
    <Flex sx={{ ...fullWidthHeight(), ...flexRowJustifyCenterAlignCenter(3) }}>
      <Box>
        <Icon name="time-clock-circle-interface-essential" size={24} />
      </Box>
      <Box>{t('Currently not booking')}</Box>
    </Flex>
  );
};
