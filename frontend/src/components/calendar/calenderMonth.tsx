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
import { addMonths, intervalToDuration, isToday, startOfWeek, toDate } from 'date-fns';
import { AnimatePresence, m } from 'framer-motion';
import { IsoDateString } from 'lib/api/apiDateHandling';
import { useTranslation } from 'next-i18next';
import { Button } from '@theme-ui/components';
import { flexRowJustifyCenterAlignCenter } from 'styles/shortcuts';
import { CalendarDay } from 'components/calendar/calendarDay';
import { SelectedTabIcon } from 'components/shared/motion/selectedTabIcon';
import { ButtonLeft } from 'components/shared/buttonLeft';
import { ButtonRight } from 'components/shared/buttonRight';
import { FormatDate } from 'components/shared/formatDate';
import { formatISOLocale, getMonthOfDate } from 'lib/dates';
import { useStore } from 'storeContext/store';
import { uniqueId } from 'lodash';
import { useIsClient } from 'usehooks-ts';

export const CalendarMonth: React.FC = () => {
  const { t } = useTranslation('common');
  const {
    dispatch,
    state: { calendar },
  } = useStore();
  const isClient = useIsClient();

  const [month, setMonth] = React.useState(getMonthOfDate(calendar.selectedDate));
  const [selectedDay, setSelectedDay] = React.useState(calendar.selectedDate);

  useEffect(() => {
    setMonth(getMonthOfDate(selectedDay));
  }, [selectedDay]);

  const getDay = (str: IsoDateString) => {
    return toDate(new Date(str)).getDate();
  };

  const nextMonth = () => {
    setMonth(getMonthOfDate(addMonths(new Date(month[0]), 1)));
  };

  const previousMonth = () => {
    setMonth(getMonthOfDate(addMonths(new Date(month[0]), -1)));
  };

  const showToday = () => {
    setSelectedDay(formatISOLocale(new Date()));
  };

  const handleDayClick = (day: IsoDateString) => {
    setSelectedDay(day);
    dispatch({ type: 'calendar.setSelectedDate', payload: day });
  };

  const topFiller = () => {
    const firstDayOfMonth = new Date(month[0]);
    const filler = intervalToDuration({
      start: startOfWeek(firstDayOfMonth, { weekStartsOn: 1 }),
      end: firstDayOfMonth,
    }).days;
    return new Array(filler).fill(() => uniqueId(), 0, filler);
  };

  if (!isClient) return null;

  return (
    <Flex
      sx={{
        label: 'CalendarMonth',
        width: '100%',
        justifyContent: 'center',
        alignItems: 'flex-start',
      }}
    >
      <Box sx={{ pt: 3, height: '100%' }}>
        <ButtonLeft aria-label={t('Last month')} onClick={() => previousMonth()} />
      </Box>
      <Box sx={{ maxWidth: 500, width: '100%' }}>
        <Flex
          sx={{
            justifyContent: 'space-between',
            borderBottom: '1px solid',
            borderBottomColor: 'containerTextColorMuted',
            fontSize: 2,
            mb: 3,
          }}
        >
          <Flex sx={{ ...flexRowJustifyCenterAlignCenter }}>
            <FormatDate date={month[0]} format="monthNameLong" />
          </Flex>
          <Box>
            {!isToday(new Date(selectedDay)) && (
              <Button
                variant="smallTransparent"
                aria-label={t('Today')}
                onClick={() => showToday()}
              >
                {t('Today')}
              </Button>
            )}
          </Box>
          <Flex sx={{ ...flexRowJustifyCenterAlignCenter }}>
            <FormatDate date={month[0]} format="year" />
          </Flex>
        </Flex>
        <AnimatePresence>
          <Grid
            sx={{
              gridTemplateColumns: 'repeat(7,1fr)',
              justifyContent: 'stretch',
              width: '100%',
              gap: 3,
              px: 3,
              pt: [0, 1, 3],
            }}
          >
            {topFiller().map((item) => (
              <Box key={item()} />
            ))}
            {month.map((day) => (
              <m.div
                key={day}
                animate={{ opacity: 1 }}
                initial={{ opacity: 0 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.5 }}
              >
                <Box
                  sx={{
                    position: 'relative',
                    flexGrow: 1,
                    color: getDay(selectedDay) === getDay(day) ? 'negativeText' : 'currentcolor',
                  }}
                >
                  {getDay(selectedDay) === getDay(day) && (
                    <SelectedTabIcon layoutId="calendarMonth" radiusOn="all" />
                  )}
                  <CalendarDay date={day} onClick={() => handleDayClick(day)} />
                </Box>
              </m.div>
            ))}
          </Grid>
        </AnimatePresence>
      </Box>
      <Box sx={{ pt: 3, height: '100%' }}>
        <ButtonRight aria-label={t('Next month')} onClick={() => nextMonth()} />
      </Box>
    </Flex>
  );
};
