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
import { Button, Flex } from 'theme-ui';
import { BookingDurationCounter } from 'layout/pages/user/index/bookingDurationCounter';
import { TagList } from 'components/shared/tagList';
import { Icon } from 'components/shared/icon';
import {
  clickableStyle,
  flexColumnJustifyCenterAlignEnd,
  flexRowJustifyBetweenAlignCenter,
  flexRowJustifyCenterAlignCenter,
  flexRowJustifyStartAlignCenter,
  fullWidthHeight,
} from 'styles/shortcuts';
import { BookingName } from 'layout/pages/user/index/bookingName';
import { BookingFrom } from 'layout/pages/user/index/bookingFrom';
import { formatISOLocale } from 'lib/dates';
import { Responsively } from 'components/shared/responsively';
import { useTranslation } from 'next-i18next';
import { BookingCurrentEntryContext } from './bookingCurrentEntryContext';
import { useRouter } from 'next/router';
import { ROUTES } from 'projectConfig/routes';
import {
  getGetUserBookingCurrentKey,
  stopUserBookingCurrent,
  useGetUserBookingCurrent,
} from 'lib/api/lasius/user-bookings/user-bookings';
import { useSWRConfig } from 'swr';
import { BookingCurrentNoBooking } from 'layout/pages/user/index/current/bookingCurrentNoBooking';
import { useIsClient } from 'usehooks-ts';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { AnimateChange } from 'components/shared/motion/animateChange';
import { useStore } from 'storeContext/store';
import { roundToNearestMinutes } from 'date-fns';

type Props = {
  inContainer?: boolean;
};
export const BookingCurrentEntry: React.FC<Props> = ({ inContainer = false }) => {
  const { t } = useTranslation('common');
  const router = useRouter();
  const { mutate } = useSWRConfig();
  const { selectedOrganisationId } = useOrganisation();
  const isClient = useIsClient();
  const store = useStore();

  const { data } = useGetUserBookingCurrent({ swr: { enabled: isClient } });

  const stop = async () => {
    if (data?.booking?.id) {
      await stopUserBookingCurrent(selectedOrganisationId, data.booking.id, {
        end: formatISOLocale(roundToNearestMinutes(new Date(), { roundingMethod: 'floor' })),
      });
      await mutate(getGetUserBookingCurrentKey());
      store.dispatch({ type: 'calendar.setSelectedDate', payload: formatISOLocale(new Date()) });
    }
  };

  const handleClick = async () => {
    if (!inContainer) await router.push(ROUTES.USER.INDEX);
  };

  return (
    <AnimateChange hash={`${!data?.booking}`} useAvailableSpace>
      {!data?.booking ? (
        <BookingCurrentNoBooking />
      ) : (
        <Flex
          sx={{
            label: 'BookingCurrentEntry',
            ...flexRowJustifyBetweenAlignCenter([2, 2, 4]),
            ...fullWidthHeight(),
            boxSizing: 'border-box',
          }}
        >
          <Flex sx={flexRowJustifyStartAlignCenter([2, 2, 3])}>
            <Button
              onClick={stop}
              variant="stopRecording"
              title={t('Stop recording current time booking')}
            >
              <Icon name="controls-stop-video-movies-tv" size={24} />
            </Button>
            <Flex
              sx={{
                flexDirection: 'column',
                gap: 1,
                lineHeight: 'normal',
                ...(!inContainer && clickableStyle()),
              }}
              onClick={handleClick}
            >
              <BookingName item={data.booking} />
              <TagList items={data.booking.tags} />
            </Flex>
          </Flex>
          <Flex sx={{ ...flexRowJustifyCenterAlignCenter([2, 2, 4]), flexShrink: 0 }}>
            <Responsively mode="show" on={['md', 'lg']}>
              <Flex sx={{ ...flexRowJustifyStartAlignCenter([2, 2, 4]), height: '100%' }}>
                <BookingFrom startDate={data.booking.start?.dateTime} />
                <BookingDurationCounter
                  startDate={data.booking.start?.dateTime || formatISOLocale(new Date())}
                />
              </Flex>
            </Responsively>
            <Responsively mode="show" on={['xs', 'sm']}>
              <Flex sx={{ ...flexColumnJustifyCenterAlignEnd(1), height: '100%' }}>
                <BookingFrom startDate={data.booking.start?.dateTime} />
                <BookingDurationCounter
                  startDate={data.booking.start?.dateTime || formatISOLocale(new Date())}
                />
              </Flex>
            </Responsively>
            <BookingCurrentEntryContext item={data.booking} />
          </Flex>
        </Flex>
      )}
    </AnimateChange>
  );
};
