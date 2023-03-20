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

import React, { useEffect } from 'react';
import { Box, Flex, Grid } from 'theme-ui';
import { addWeeks, isToday, toDate } from 'date-fns';
import { IsoDateString } from 'lib/api/apiDateHandling';
import { ButtonLeft } from 'components/shared/buttonLeft';
import { ButtonRight } from 'components/shared/buttonRight';
import { useTranslation } from 'next-i18next';
import { Button } from '@theme-ui/components';
import { SelectedTabIcon } from 'components/shared/motion/selectedTabIcon';
import { CalendarDay } from 'components/calendar/calendarDay';
import { FormatDate } from 'components/shared/formatDate';
import { formatISOLocale, getWeekOfDate } from 'lib/dates';
import { useIsClient } from 'usehooks-ts';
import { useStore } from 'storeContext/store';
import { AnimateChange } from 'components/shared/motion/animateChange';

export const CalendarWeekResponsive: React.FC = () => {
  const { t } = useTranslation('common');
  const store = useStore();

  const [week, setWeek] = React.useState(getWeekOfDate(formatISOLocale(new Date())));
  const [selectedDay, setSelectedDay] = React.useState(
    store.state.calendar.selectedDate || formatISOLocale(new Date())
  );
  const isClient = useIsClient();

  useEffect(() => {
    setWeek(getWeekOfDate(selectedDay));
  }, [selectedDay]);

  if (!isClient) return null;

  const getDay = (str: IsoDateString) => {
    return toDate(new Date(str)).getDate();
  };

  const nextWeek = () => {
    setWeek(getWeekOfDate(addWeeks(new Date(week[0]), 1)));
  };

  const previousWeek = () => {
    setWeek(getWeekOfDate(addWeeks(new Date(week[0]), -1)));
  };

  const handleDayClick = (day: IsoDateString) => {
    setSelectedDay(day);
    store.dispatch({ type: 'calendar.setSelectedDate', payload: day });
  };

  const showToday = () => {
    handleDayClick(formatISOLocale(new Date()));
  };

  return (
    <Flex
      sx={{
        label: 'CalendarWeekResponsive',
        width: '100%',
        height: '100%',
        justifyContent: 'center',
        alignItems: 'center',
        overflow: 'hidden',
      }}
    >
      <Flex sx={{ pt: 3, height: '100%', justifyContent: 'center', alignItems: 'center' }}>
        <ButtonLeft aria-label={t('Last week')} onClick={() => previousWeek()} />
      </Flex>
      <Box sx={{ maxWidth: 500, width: '100%' }}>
        <Grid
          sx={{
            width: '100%',
            gridTemplateColumns: 'repeat(3, 1fr)',
            borderBottom: '1px solid',
            borderBottomColor: 'containerTextColorMuted',
            fontSize: 1,
            minHeight: 22,
          }}
        >
          <Box sx={{}}>
            <FormatDate date={week[0]} format="monthNameLong" />
          </Box>
          {!isToday(new Date(selectedDay)) ? (
            <Button variant="smallTransparent" aria-label={t('Today')} onClick={() => showToday()}>
              {t('Today')}
            </Button>
          ) : (
            <Box />
          )}
          <Box sx={{ textAlign: 'right' }}>
            <FormatDate date={week[0]} format="year" />
          </Box>
        </Grid>
        <Box
          sx={{
            width: '100%',
            overflowY: ['scroll', 'scroll', 'hidden'],
            overflowScrolling: 'touch',
            scrollBehavior: 'smooth',
            minHeight: 82,
          }}
        >
          <AnimateChange hash={week[0]}>
            <Grid
              sx={{
                width: '100%',
                gridTemplateColumns: ['repeat(7, 62px)', 'repeat(7, 62px)', 'repeat(7, 1fr)'],
                gap: [1, 2, 2, 3],
              }}
            >
              {week.map((day) => (
                <Box
                  key={day}
                  sx={{
                    position: 'relative',
                    color: getDay(selectedDay) === getDay(day) ? 'negativeText' : 'currentcolor',
                  }}
                >
                  {getDay(selectedDay) === getDay(day) && (
                    <SelectedTabIcon layoutId="calendarDay" radiusOn="bottom" />
                  )}
                  <CalendarDay date={day} onClick={() => handleDayClick(day)} />
                </Box>
              ))}
            </Grid>
          </AnimateChange>
        </Box>
      </Box>
      <Flex sx={{ pt: 3, height: '100%', justifyContent: 'center', alignItems: 'center' }}>
        <ButtonRight aria-label={t('Next week')} onClick={() => nextWeek()} />
      </Flex>
    </Flex>
  );
};
