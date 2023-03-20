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

import { ModelsTags } from 'types/common';
import { FormProvider, useForm } from 'react-hook-form';
import { formatISOLocale } from 'lib/dates';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useGetOrganisationBookingList } from 'lib/api/lasius/organisation-bookings/organisation-bookings';
import { apiTimespanFromTo } from 'lib/api/apiDateHandling';
import useScrollPagination from 'lib/hooks/useScrollPaginationHook';
import { layoutColumnStyle } from 'styles/shortcuts';
import { themeRadii } from 'styles/theme/radii';
import React, { useMemo } from 'react';
import { ScrollContainer } from 'components/scrollContainer';
import { Loading } from 'components/shared/fetchState/loading';
import { BookingHistoryTable } from 'components/bookingHistory/bookingHistoryTable';
import { ColumnList } from 'components/shared/columnList';
import { BookingHistoryFilter } from 'components/bookingHistory/bookingHistoryFilter';
import { BookingHistoryExport } from 'components/bookingHistory/bookingHistoryExport';
import { useGetUserBookingListByOrganisation } from 'lib/api/lasius/user-bookings/user-bookings';
import { ModelsBooking } from 'lib/api/lasius';
import { ErrorType } from 'lib/api/lasiusAxiosInstance';
import { KeyedMutator } from 'swr';
import { BookingHistoryStats } from 'components/bookingHistory/bookingHistoryStats';
import { getModelsBookingSummary } from 'lib/api/functions/getModelsBookingSummary';
import { filterModelsBookingListProjectId } from 'lib/api/functions/filterModelsBookingListProjectId';
import { filterModelsBookingListByTags } from 'lib/api/functions/filterModelsBookingListByTags';
import { getExtendedModelsBookingList } from 'lib/api/functions/getExtendedModelsBookingList';
import { useIsClient } from 'usehooks-ts';

type FormValues = {
  projectId: string;
  tags: ModelsTags[];
  from: string;
  to: string;
  dateRange: string;
};

type Props = {
  dataSource: 'userBookings' | 'organisationBookings';
};
export const BookingHistoryLayout: React.FC<Props> = ({ dataSource }) => {
  const isClient = useIsClient();

  const hookForm = useForm<FormValues>({
    defaultValues: {
      projectId: '',
      tags: [],
      from: formatISOLocale(new Date()),
      to: formatISOLocale(new Date()),
      dateRange: '',
    },
  });
  const { selectedOrganisationId } = useOrganisation();

  let response: {
    data?: ModelsBooking[];
    isValidating: any;
    error?: ErrorType<unknown> | undefined;
    mutate?: KeyedMutator<ModelsBooking[]>;
    swrKey?: string | false | Record<any, any>;
  };

  switch (dataSource) {
    case 'userBookings':
      // eslint-disable-next-line react-hooks/rules-of-hooks
      response = useGetUserBookingListByOrganisation(selectedOrganisationId, {
        ...apiTimespanFromTo(hookForm.getValues('from'), hookForm.getValues('to')),
      });
      break;
    case 'organisationBookings':
    default:
      // eslint-disable-next-line react-hooks/rules-of-hooks
      response = useGetOrganisationBookingList(selectedOrganisationId, {
        ...apiTimespanFromTo(hookForm.getValues('from'), hookForm.getValues('to')),
      });
      break;
  }

  const tags = hookForm.watch('tags');
  const projectId = hookForm.watch('projectId');

  const processedItems = useMemo(() => {
    return getExtendedModelsBookingList(
      filterModelsBookingListProjectId(
        filterModelsBookingListByTags(response.data || [], tags),
        projectId
      )
    );
  }, [response.data, tags, projectId]);

  const summary = useMemo(() => {
    return getModelsBookingSummary(processedItems);
  }, [processedItems]);

  const { onScroll, visibleElements } = useScrollPagination(processedItems);

  const allowEdit = dataSource === 'userBookings';
  const allowDelete = dataSource === 'userBookings';
  const showUserColumn = dataSource === 'organisationBookings';

  if (!isClient) return null;

  return (
    <FormProvider {...hookForm}>
      <ScrollContainer
        sx={{
          ...layoutColumnStyle,
          pt: 4,
        }}
        onScroll={onScroll}
      >
        {!response.data && response.isValidating && <Loading />}
        {response.data && (
          <BookingHistoryTable
            items={visibleElements}
            allowEdit={allowEdit}
            allowDelete={allowDelete}
            showUserColumn={showUserColumn}
          />
        )}
      </ScrollContainer>
      <ScrollContainer
        sx={{
          ...layoutColumnStyle,
          background: 'containerBackgroundDarker',
          borderTopRightRadius: themeRadii.large,
        }}
      >
        <ColumnList>
          <BookingHistoryStats hours={summary.hours} bookings={summary.elements} />
          <BookingHistoryFilter />
          <BookingHistoryExport bookings={processedItems} />
        </ColumnList>
      </ScrollContainer>
    </FormProvider>
  );
};
