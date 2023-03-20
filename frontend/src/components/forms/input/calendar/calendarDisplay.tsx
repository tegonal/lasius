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

import React, { useEffect, useState } from 'react';
import { Box, Flex, Grid } from 'theme-ui';
import { addMonths, intervalToDuration, setHours, setMinutes, startOfWeek, toDate } from 'date-fns';
import { IsoDateString } from 'lib/api/apiDateHandling';
import { useTranslation } from 'next-i18next';
import { Button } from '@theme-ui/components';
import { flexRowJustifyCenterAlignCenter, fullWidthHeight } from 'styles/shortcuts';
import { ButtonLeft } from 'components/shared/buttonLeft';
import { ButtonRight } from 'components/shared/buttonRight';
import { formatISOLocale, getMonthOfDate, getWeekOfDate } from 'lib/dates';
import { CalendarDayCompact } from 'components/forms/input/calendar/calendarDayCompact';
import { SelectedTabStatic } from 'components/shared/motion/selectedTabStatic';
import { FormatDate } from 'components/shared/formatDate';
import { uniqueId } from 'lodash';
import { AnimateChange } from 'components/shared/motion/animateChange';

type Props = {
  onChange: (date: IsoDateString) => void;
  value: IsoDateString;
};
export const CalendarDisplay: React.FC<Props> = ({ value, onChange }) => {
  const { t } = useTranslation('common');
  const [referenceDay, setReferenceDay] = useState<IsoDateString>(formatISOLocale(new Date()));
  const [selectedDay, setSelectedDay] = useState<IsoDateString>(referenceDay);
  const [currentMonth, setCurrentMonth] = useState<IsoDateString[]>(getMonthOfDate(referenceDay));
  const [firstDayOfMonth, setFirstDayOfMonth] = useState<Date>(new Date(currentMonth[0]));
  const [topFiller, setTopFiller] = useState<any[]>([]);
  const [originalTime, setOrignalTime] = useState<number[]>([0]);

  useEffect(() => {
    if (value) {
      setReferenceDay(value);
      setSelectedDay(value);
      setOrignalTime([new Date(value).getHours(), new Date(value).getMinutes()]);
    }
  }, [value]);

  const getDay = (str: IsoDateString) => {
    return toDate(new Date(str)).getDate();
  };

  const nextMonth = () => {
    setReferenceDay(formatISOLocale(addMonths(new Date(referenceDay), 1)));
  };

  const previousMonth = () => {
    setReferenceDay(formatISOLocale(addMonths(new Date(referenceDay), -1)));
  };

  const showToday = () => {
    setReferenceDay(formatISOLocale(new Date()));
    setSelectedDay(referenceDay);
  };

  useEffect(() => {
    setCurrentMonth(getMonthOfDate(referenceDay));
  }, [referenceDay]);

  useEffect(() => {
    setFirstDayOfMonth(new Date(currentMonth[0]));
  }, [currentMonth, currentMonth.length]);

  useEffect(() => {
    const filler = intervalToDuration({
      start: startOfWeek(firstDayOfMonth, { weekStartsOn: 1 }),
      end: firstDayOfMonth,
    }).days;
    setTopFiller(new Array(filler).fill(() => uniqueId(), 0, filler));
  }, [firstDayOfMonth]);

  const handleChange = (date: IsoDateString) => {
    const dateObj = new Date(date);
    onChange(formatISOLocale(setMinutes(setHours(dateObj, originalTime[0]), originalTime[1])));
  };

  return (
    <Grid
      sx={{
        label: 'CalendarDisplay',
        gridTemplateColumns: '48px auto 48px',
        gap: 1,
        color: 'text',
        userSelect: 'none',
      }}
    >
      <Box sx={{ pt: 3 }}>
        <ButtonLeft aria-label={t('Previous month')} onClick={() => previousMonth()} />
      </Box>
      <Box sx={{ width: '100%' }}>
        <Flex
          sx={{
            justifyContent: 'space-between',
            borderBottom: '1px solid',
            borderBottomColor: 'containerTextColorMuted',
            fontSize: 1,
          }}
        >
          <Flex sx={{ ...flexRowJustifyCenterAlignCenter }}>
            <FormatDate date={firstDayOfMonth} format="monthNameLong" />
          </Flex>
          <Box>
            <Button variant="smallTransparent" aria-label={t('Today')} onClick={() => showToday()}>
              {t('Today')}
            </Button>
          </Box>
          <Flex sx={{ ...flexRowJustifyCenterAlignCenter }}>
            <FormatDate date={firstDayOfMonth} format="year" />
          </Flex>
        </Flex>
        <AnimateChange hash={referenceDay}>
          <Grid
            sx={{
              gridTemplateColumns: 'repeat(7,1fr)',
              justifyContent: 'stretch',
              width: '100%',
              gap: 1,
              px: 1,
            }}
          >
            {getWeekOfDate(selectedDay).map((week) => (
              <Box
                key={`weekday-${week}`}
                sx={{
                  fontSize: '8px',
                  fontWeight: 500,
                  lineHeight: 'normal',
                  py: 1,
                  textTransform: 'uppercase',
                  textAlign: 'center',
                }}
              >
                <FormatDate date={week} format="dayNameShort" />
              </Box>
            ))}
          </Grid>
          <Grid
            sx={{
              gridTemplateColumns: 'repeat(7,1fr)',
              justifyContent: 'stretch',
              width: '100%',
              gap: 1,
              px: 1,
            }}
          >
            {topFiller.map((item) => (
              <Box key={item()} />
            ))}
            {currentMonth.map((day) => (
              <Box
                key={`day${day}`}
                sx={{
                  position: 'relative',
                  flexGrow: 1,
                  ...fullWidthHeight(),
                  color: getDay(selectedDay) === getDay(day) ? 'negativeText' : 'currentcolor',
                }}
              >
                {getDay(selectedDay) === getDay(day) && <SelectedTabStatic radiusOn="all" />}
                <CalendarDayCompact date={day} onClick={() => handleChange(day)} />
              </Box>
            ))}
          </Grid>
        </AnimateChange>
      </Box>
      <Box sx={{ pt: 3 }}>
        <ButtonRight aria-label={t('Next month')} onClick={() => nextMonth()} />
      </Box>
    </Grid>
  );
};
