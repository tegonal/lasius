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
import { ContainerColumnsDesktop } from 'layout/containerColumnsDesktop';
import { Logo } from 'components/logo';
import { Box, Flex } from 'theme-ui';
import { clickableStyle, fullWidthHeight } from 'styles/shortcuts';
import { CalendarWeekResponsive } from 'components/calendar/calendarWeekResponsive';
import { BookingCurrent } from 'layout/pages/user/index/current/bookingCurrent';
import { ColorModeToggle } from 'components/colorModeToggle';
import { SelectUserOrganisation } from 'components/shared/selectUserOrganisation';
import { useRouter } from 'next/router';
import { ROUTES } from 'projectConfig/routes';
import { AnimateChange } from 'components/shared/motion/animateChange';

export const HeaderDesktop: React.FC = () => {
  const router = useRouter();
  const showCalendar = router.route === '/user/home';
  return (
    <Box as="section" sx={{ label: 'HeaderDesktop', ...fullWidthHeight() }}>
      <ContainerColumnsDesktop>
        <Flex
          sx={{
            alignItems: 'center',
            gap: 4,
            justifyContent: 'flex-start',
            pl: 4,
            ...clickableStyle(),
          }}
          onClick={() => router.push(ROUTES.USER.INDEX)}
        >
          <Logo />
        </Flex>
        <AnimateChange hash={showCalendar ? 'calendar' : 'booking'} useAvailableSpace>
          <Flex
            sx={{ alignItems: 'center', gap: 4, justifyContent: 'center', ...fullWidthHeight() }}
          >
            {showCalendar ? <CalendarWeekResponsive /> : <BookingCurrent inContainer={false} />}
          </Flex>
        </AnimateChange>
        <Flex sx={{ alignItems: 'center', gap: 4, justifyContent: 'flex-end', pr: 4 }}>
          <SelectUserOrganisation />
          <ColorModeToggle />
        </Flex>
      </ContainerColumnsDesktop>
    </Box>
  );
};
