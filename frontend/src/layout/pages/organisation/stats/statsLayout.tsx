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
import { layoutColumnStyle } from 'styles/shortcuts';
import { themeRadii } from 'styles/theme/radii';
import { StatsFilter } from 'components/shared/stats/statsFilter';
import { ColumnList } from 'components/shared/columnList';
import { ScrollContainer } from 'components/scrollContainer';
import { FormProvider, useForm } from 'react-hook-form';
import { formatISOLocale } from 'lib/dates';
import { dateOptions } from 'lib/dateOptions';
import { StatsOverview } from 'layout/pages/organisation/stats/statsOverview';
import { StatsContent } from 'layout/pages/organisation/stats/statsContent';

type FormValues = {
  to: string;
  from: string;
  dateRange: string;
};

export const StatsLayout: React.FC = () => {
  const hookForm = useForm<FormValues>({
    defaultValues: {
      from: formatISOLocale(new Date()),
      to: formatISOLocale(new Date()),
      dateRange: dateOptions[0].name,
    },
  });

  return (
    <FormProvider {...hookForm}>
      <ScrollContainer sx={{ ...layoutColumnStyle }}>
        <StatsContent />
      </ScrollContainer>
      <ScrollContainer
        sx={{
          ...layoutColumnStyle,
          background: 'containerBackgroundDarker',
          borderTopRightRadius: themeRadii.large,
        }}
      >
        <ColumnList>
          <StatsOverview />
          <StatsFilter />
        </ColumnList>
      </ScrollContainer>
    </FormProvider>
  );
};
