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
import { BookingItem } from 'layout/pages/user/index/list/bookingItem';
import { BookingListWrapper } from 'layout/pages/user/index/list/bookingListWrapper';
import { useGetUserBookingListByOrganisation } from 'lib/api/lasius/user-bookings/user-bookings';
import { apiTimespanDay } from 'lib/api/apiDateHandling';
import { DataFetchEmpty } from 'components/shared/fetchState/dataFetchEmpty';
import { AnimateList } from 'components/shared/motion/animateList';
import { DataFetchValidates } from 'components/shared/fetchState/dataFetchValidates';
import { useIsClient } from 'usehooks-ts';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useStore } from 'storeContext/store';
import { stringHash } from 'lib/stringHash';
import { sortBookingsByDate } from 'lib/api/functions/sortBookingsByDate';

export const BookingListSelectedDay: React.FC = () => {
  const { selectedOrganisationId } = useOrganisation();
  const isClient = useIsClient();
  const {
    state: { calendar },
  } = useStore();

  const { data, isValidating } = useGetUserBookingListByOrganisation(
    selectedOrganisationId,
    apiTimespanDay(calendar.selectedDate),
    {
      swr: {
        enabled: !!selectedOrganisationId,
      },
    }
  );

  const sortedList = useMemo(() => sortBookingsByDate(data || []), [data]);

  if (!isClient) return null;

  const hasNoData = !data || data?.length === 0;

  return (
    <BookingListWrapper>
      <DataFetchValidates isValidating={isValidating} />
      {hasNoData ? (
        <DataFetchEmpty />
      ) : (
        <AnimateList hash={calendar.selectedDate}>
          {sortedList.map((item) => (
            <BookingItem key={stringHash(item)} item={item} />
          ))}
        </AnimateList>
      )}
    </BookingListWrapper>
  );
};
