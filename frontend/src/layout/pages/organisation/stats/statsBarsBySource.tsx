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
import { StatsTile } from 'components/charts/statsTile';
import { BarChartGroupMode } from 'components/charts/barsHours';
import { apiDatespanFromTo } from 'lib/api/apiDateHandling';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useFormContext } from 'react-hook-form';
import { Loading } from 'components/shared/fetchState/loading';
import { DataFetchEmpty } from 'components/shared/fetchState/dataFetchEmpty';
import { useGetOrganisationStatsBySourceAndDay } from 'lib/api/hooks/useGetOrganisationStatsBySourceAndDay';
import { OrganisationBookingSource } from 'types/booking';
import dynamic from 'next/dynamic';

type Props = {
  source: OrganisationBookingSource;
  groupMode: BarChartGroupMode;
};

const BarsHours = dynamic(() => import('../../../../components/charts/barsHours'), { ssr: false });

export const StatsBarsBySource: React.FC<Props> = ({ source, groupMode }) => {
  const { selectedOrganisationId } = useOrganisation();
  const parentFormContext = useFormContext();

  const { data, isValidating } = useGetOrganisationStatsBySourceAndDay(selectedOrganisationId, {
    source,
    ...apiDatespanFromTo(parentFormContext.watch('from'), parentFormContext.watch('to')),
  });

  if (isValidating) {
    return (
      <StatsTile sx={{ height: 240 }}>
        <Loading />
      </StatsTile>
    );
  }

  if (!data) {
    return (
      <StatsTile sx={{ height: 240 }}>
        <DataFetchEmpty />
      </StatsTile>
    );
  }

  return (
    <StatsTile sx={{ height: 240 }}>
      <BarsHours stats={data} indexBy="category" groupMode={groupMode} />
    </StatsTile>
  );
};
