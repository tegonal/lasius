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

import React, { useMemo } from 'react';
import { StatsTile } from 'components/charts/statsTile';
import { apiDatespanFromTo } from 'lib/api/apiDateHandling';
import { useGetUserBookingAggregatedStatsByOrganisation } from 'lib/api/lasius/user-bookings/user-bookings';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useFormContext } from 'react-hook-form';
import { getTransformedChartDataAggregate } from 'lib/api/functions/getTransformedChartDataAggregate';
import { Loading } from 'components/shared/fetchState/loading';
import { DataFetchEmpty } from 'components/shared/fetchState/dataFetchEmpty';
import { statsSwrConfig } from 'components/shared/stats/statsSwrConfig';
import dynamic from 'next/dynamic';

const PieDiagram = dynamic(() => import('../../../../components/charts/pieDiagram'), {
  ssr: false,
});

export const StatsCircleCategoryRange: React.FC = () => {
  const { selectedOrganisationId } = useOrganisation();
  const parentFormContext = useFormContext();

  const { data, isValidating } = useGetUserBookingAggregatedStatsByOrganisation(
    selectedOrganisationId,
    {
      source: 'project',
      ...apiDatespanFromTo(parentFormContext.watch('from'), parentFormContext.watch('to')),
      granularity: 'All',
    },
    statsSwrConfig
  );

  const chartData = useMemo(() => getTransformedChartDataAggregate(data), [data]);

  if (isValidating) {
    return (
      <StatsTile sx={{ height: 340 }}>
        <Loading />
      </StatsTile>
    );
  }

  if (!chartData.data) {
    return (
      <StatsTile sx={{ height: 340 }}>
        <DataFetchEmpty />
      </StatsTile>
    );
  }

  return (
    <StatsTile sx={{ height: 340 }}>
      <PieDiagram stats={chartData} />
    </StatsTile>
  );
};
