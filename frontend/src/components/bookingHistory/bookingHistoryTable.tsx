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
import { useTranslation } from 'next-i18next';
import { DataList } from 'components/dataList/dataList';
import { DataListHeaderItem } from 'components/dataList/dataListHeaderItem';
import { DataListField } from 'components/dataList/dataListField';
import { TagList } from 'components/shared/tagList';
import { Text } from '@theme-ui/components';
import { clickableStyle } from 'styles/shortcuts';
import { DataListRow } from 'components/dataList/dataListRow';
import { ModelsTags } from 'types/common';
import { DataFetchEmpty } from 'components/shared/fetchState/dataFetchEmpty';
import { useFormContext } from 'react-hook-form';
import { BookingHistoryItemContext } from 'components/bookingHistory/bookingHistoryItemContext';
import { ExtendedHistoryBooking } from 'types/booking';
import { ModelsBooking } from 'lib/api/lasius';
import { sortExtendedBookingsByDate } from 'lib/api/functions/sortBookingsByDate';

type Props = {
  items: ExtendedHistoryBooking[];
  allowEdit?: boolean;
  allowDelete?: boolean;
  showUserColumn?: boolean;
};

export const BookingHistoryTable: React.FC<Props> = ({
  items,
  allowDelete,
  allowEdit,
  showUserColumn,
}) => {
  const { t } = useTranslation('common');
  const formContext = useFormContext();

  const tagClickHandler = (tag: ModelsTags) => {
    if (tag) {
      const tags = formContext.getValues('tags');
      formContext.setValue('tags', [...tags, tag]);
    }
  };

  const projectIdClickHandler = (booking: ModelsBooking) => {
    const {
      projectReference: { id },
    } = booking;
    if (id) {
      formContext.setValue('projectId', id);
    }
  };

  const sortedList = useMemo(() => sortExtendedBookingsByDate(items), [items]);

  if (sortedList.length < 1) return <DataFetchEmpty />;

  return (
    <DataList>
      <DataListRow>
        {showUserColumn && <DataListHeaderItem>{t('User')}</DataListHeaderItem>}
        <DataListHeaderItem>{t('Project')}</DataListHeaderItem>
        <DataListHeaderItem>{t('Tags')}</DataListHeaderItem>
        <DataListHeaderItem>{t('Date')}</DataListHeaderItem>
        <DataListHeaderItem>{t('Duration')}</DataListHeaderItem>
        <DataListHeaderItem />
      </DataListRow>
      {sortedList.map((booking) => (
        <DataListRow key={booking.id}>
          {showUserColumn && (
            <DataListField sx={{ whiteSpace: 'nowrap' }}>{booking.userReference.key}</DataListField>
          )}
          <DataListField>
            <Text
              sx={{ ...clickableStyle() }}
              data-value={booking.projectReference.key}
              onClick={() => projectIdClickHandler(booking)}
            >
              {booking.projectReference.key}
            </Text>
          </DataListField>
          <DataListField>
            <TagList items={booking.tags} clickHandler={tagClickHandler} hideRemoveIcon />
          </DataListField>
          <DataListField sx={{ whiteSpace: 'nowrap' }}>
            <Text variant="small">{booking.fromTo}</Text> <br />
            <Text variant="small">{booking.date}</Text>
          </DataListField>
          <DataListField sx={{ whiteSpace: 'nowrap' }}>{booking.durationString}</DataListField>
          <DataListField>
            <BookingHistoryItemContext
              item={booking}
              allowDelete={allowDelete}
              allowEdit={allowEdit}
            />
          </DataListField>
        </DataListRow>
      ))}
    </DataList>
  );
};
