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
import { Box } from 'theme-ui';
import { BookingCurrent } from 'layout/pages/user/index/current/bookingCurrent';
import { fullWidthHeight } from 'styles/shortcuts';
import { themeRadii } from 'styles/theme/radii';
import { effects } from 'styles/theme/effects';
import { BookingListSelectedDay } from 'layout/pages/user/index/list/bookingListSelectedDay';
import { BookingAddMobileButton } from 'layout/pages/user/index/bookingAddMobileButton';
import { ZINDEX } from 'styles/themeConstants';
import { MobileNavigationButton } from 'components/navigation/mobile/mobileNavigationButton';
import { BookingDayStatsProgressBar } from 'layout/pages/user/index/bookingDayStatsProgressBar';

export const HomeLayoutMobile: React.FC = () => {
  return (
    <>
      <Box
        as="section"
        sx={{
          label: 'IndexLayoutMobile',
          display: 'grid',
          gridTemplateRows: 'min-content min-content auto',
          gap: 1,
          ...fullWidthHeight(),
          background: 'containerBackground',
          color: 'containerTextColor',
          overflow: 'auto',
          ...effects.shadows.softShadowOnWhiteUp,
          borderTopLeftRadius: themeRadii.large,
          borderTopRightRadius: themeRadii.large,
          position: 'relative',
        }}
      >
        <BookingCurrent />
        <BookingDayStatsProgressBar />
        <BookingListSelectedDay />
      </Box>
      <Box
        sx={{
          zIndex: ZINDEX.SIDEBAR_RIGHT,
          position: 'fixed',
          bottom: 3,
          left: 3,
        }}
      >
        <MobileNavigationButton />
      </Box>
      <Box
        sx={{
          zIndex: ZINDEX.SIDEBAR_RIGHT,
          position: 'fixed',
          bottom: 3,
          left: '50%',
          transform: 'translateX(-50%)',
        }}
      >
        <BookingAddMobileButton />
      </Box>
    </>
  );
};
