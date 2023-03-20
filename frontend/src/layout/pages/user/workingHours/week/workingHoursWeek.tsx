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
import { Box, Flex, Grid, Heading } from 'theme-ui';
import { fullWidthHeight } from 'styles/shortcuts';
import { themeRadii } from 'styles/theme/radii';
import {
  decimalHoursToDate,
  decimalHoursToDurationStringRounded,
  getWeekOfDate,
  getWorkingHoursWeekdayString,
} from 'lib/dates';
import { dateFormat } from 'projectConfig/dateFormat';
import { ModelsUserOrganisation } from 'lib/api/lasius';
import { WorkingHoursDay } from 'layout/pages/user/workingHours/week/workingHoursDay';
import { AvatarOrganisation } from 'components/shared/avatar/avatarOrganisation';
import { plannedWorkingHoursStub } from 'lib/stubPlannedWorkingHours';
import { ModelsWorkingHoursWeekdays } from 'types/common';
import { IsoDateString } from 'lib/api/apiDateHandling';
import { useTranslation } from 'next-i18next';

type Props = {
  organisation: ModelsUserOrganisation;
};

type Week = {
  day: ModelsWorkingHoursWeekdays;
  date: IsoDateString;
  value: IsoDateString;
  displayValue: string;
};

export const WorkingHoursWeek: React.FC<Props> = ({ organisation }) => {
  const { t } = useTranslation('common');
  let weeklyTotal = 0;
  const initialPlannedWorkingHours = organisation.plannedWorkingHours
    ? organisation.plannedWorkingHours
    : { ...plannedWorkingHoursStub };
  const initialWeek = getWeekOfDate(new Date()).map((date) => {
    const day = getWorkingHoursWeekdayString(date);
    const hours = initialPlannedWorkingHours[day];
    weeklyTotal += hours;
    return {
      day,
      date,
      value: decimalHoursToDate(hours),
      displayValue: dateFormat(decimalHoursToDate(hours), 'HH:mm'),
    };
  });
  const week: Week[] = initialWeek;
  const totalWeek: string = decimalHoursToDurationStringRounded(weeklyTotal);

  return (
    <Box sx={{ label: 'WorkingHoursWeek', width: '100%' }}>
      <Heading as="h3" variant="headingUnderlined">
        <Flex sx={{ gap: 2 }}>
          <AvatarOrganisation name={organisation.organisationReference.key} size={24} />
          {organisation.private
            ? t('My personal organisation')
            : organisation.organisationReference.key}
        </Flex>
        <Box sx={{ fontWeight: 400, fontSize: 1 }}>{totalWeek}</Box>
      </Heading>
      <Grid
        sx={{
          label: 'WorkingHoursWeek',
          background: 'backgroundContainer',
          borderTopLeftRadius: themeRadii.large,
          borderTopRightRadius: themeRadii.large,
          ...fullWidthHeight(),
          gridTemplateColumns: 'repeat(7,1fr)',
          pb: 1,
        }}
      >
        {week.length > 0 &&
          week.map((item) => (
            <WorkingHoursDay key={item.date} item={item} organisation={organisation} />
          ))}
      </Grid>
    </Box>
  );
};
