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
import { Flex, Grid, useColorMode } from 'theme-ui';
import { BookingCurrent } from 'layout/pages/user/index/current/bookingCurrent';
import { layoutColumnStyle } from 'styles/shortcuts';
import { themeRadii } from 'styles/theme/radii';
import { BookingListSelectedDay } from 'layout/pages/user/index/list/bookingListSelectedDay';
import { IndexColumnTabs } from 'layout/pages/user/index/indexColumnTabs';
import { BookingDayStatsProgressBar } from 'layout/pages/user/index/bookingDayStatsProgressBar';
import { ScrollContainer } from 'components/scrollContainer';
import { effects } from 'styles/theme/effects';

export const HomeLayoutDesktop: React.FC = () => {
  const [colorMode] = useColorMode();
  return (
    <>
      <Grid
        sx={{
          ...layoutColumnStyle,
          boxShadow:
            colorMode === 'light'
              ? effects.shadows.softShadowOnWhite.boxShadow
              : effects.shadows.softShadowOnDark.boxShadow,
          gridTemplateRows: 'min-content min-content auto',
          background: 'containerBackground',
          color: 'containerTextColor',
          width: '100%',
          overflow: 'auto',
          gap: 1,
        }}
      >
        <BookingDayStatsProgressBar />
        <BookingCurrent />
        <ScrollContainer>
          <BookingListSelectedDay />
        </ScrollContainer>
      </Grid>
      <Flex
        sx={{
          ...layoutColumnStyle,
          boxShadow:
            colorMode === 'light'
              ? effects.shadows.softShadowOnWhite.boxShadow
              : effects.shadows.softShadowOnDark.boxShadow,
          background: 'containerBackgroundDarker',
          borderTopRightRadius: themeRadii.large,
          overflow: 'auto',
        }}
      >
        <IndexColumnTabs />
      </Flex>
    </>
  );
};
