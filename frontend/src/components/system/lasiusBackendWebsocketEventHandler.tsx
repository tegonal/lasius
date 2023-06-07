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
import { useLasiusWebsocket } from 'lib/api/hooks/useLasiusWebsocket';
import { logger } from 'lib/logger';
import { WEBSOCKET_EVENT } from 'projectConfig/constants';
import { useSWRConfig } from 'swr';
import {
  getGetUserBookingCurrentKey,
  getGetUserBookingCurrentListByOrganisationKey,
} from 'lib/api/lasius/user-bookings/user-bookings';
import { getGetFavoriteBookingListKey } from 'lib/api/lasius/user-favorites/user-favorites';
import { useSwrMutateMany } from 'lib/api/swrMutateMany';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { stringHash } from 'lib/stringHash';
import { useToast } from 'components/toasts/hooks/useToast';
import { useTranslation } from 'next-i18next';

export const LasiusBackendWebsocketEventHandler: React.FC = () => {
  const { mutate } = useSWRConfig();
  const { lastMessage } = useLasiusWebsocket();
  const mutateMany = useSwrMutateMany();
  const [lastMessageHash, setLastMessageHash] = React.useState<string | null>(null);
  const { selectedOrganisationId } = useOrganisation();
  const { addToast } = useToast();
  const { t } = useTranslation('common');

  const newMessage = stringHash(lastMessage) !== lastMessageHash;

  useEffect(() => {
    if (lastMessage && stringHash(lastMessage) !== lastMessageHash) {
      logger.log('[AppWebsocketEventHandler]', lastMessage);
      const { type, data } = lastMessage;

      //  Mutate data, grouped, to save requests

      switch (true) {
        case type === WEBSOCKET_EVENT.CurrentUserTimeBookingEvent:
          mutate(getGetUserBookingCurrentKey());
          mutate(getGetUserBookingCurrentListByOrganisationKey(selectedOrganisationId));
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingHistoryEntryRemoved:
        case type === WEBSOCKET_EVENT.UserTimeBookingHistoryEntryAdded:
        case type === WEBSOCKET_EVENT.UserTimeBookingHistoryEntryChanged:
          mutateMany(/.*\/user-bookings\/.*/);
          // this.booking.loadBookingsCache();
          // this.userBookingHistory.load();
          break;

        case type === WEBSOCKET_EVENT.HelloClient:
          logger.log('[AppWebsocketEventHandler][pong!]');
          break;

        case type === WEBSOCKET_EVENT.FavoriteAdded:
        case type === WEBSOCKET_EVENT.FavoriteRemoved:
          mutate(getGetFavoriteBookingListKey(selectedOrganisationId));
          break;

        case type === WEBSOCKET_EVENT.LatestTimeBooking:
        case type === WEBSOCKET_EVENT.CurrentOrganisationTimeBookings:
          //
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingByProjectEntryAdded:
        case type === WEBSOCKET_EVENT.UserTimeBookingByProjectEntryRemoved:
        case type === WEBSOCKET_EVENT.UserTimeBookingByTagEntryRemoved:
        case type === WEBSOCKET_EVENT.UserTimeBookingByTagEntryAdded:
          //  Unhandled events - these clog up the ws connection on simple updates
          break;

        default:
          logger.warn('[AppWebsocketEventHandler][UnhandledEvent]', type, { data });
      }

      //  Fire toasts on specific events

      switch (true) {
        case type === WEBSOCKET_EVENT.CurrentUserTimeBookingEvent:
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingHistoryEntryRemoved:
          addToast({ message: t('Booking removed'), type: 'SUCCESS' });
          break;

        case type === WEBSOCKET_EVENT.UserLoggedOutV2:
          logger.info('[AppWebsocketEventHandler][UserLoggedOutV2]');
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingHistoryEntryAdded:
          addToast({ message: t('Booking added'), type: 'SUCCESS' });
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingHistoryEntryChanged:
          addToast({ message: t('Booking updated'), type: 'SUCCESS' });

          break;

        case type === WEBSOCKET_EVENT.HelloClient:
          break;

        case type === WEBSOCKET_EVENT.FavoriteAdded:
          addToast({ message: t('Booking added to favorites'), type: 'SUCCESS' });
          break;

        case type === WEBSOCKET_EVENT.FavoriteRemoved:
          addToast({ message: t('Favorite removed'), type: 'SUCCESS' });
          break;

        case type === WEBSOCKET_EVENT.LatestTimeBooking:
          addToast({ message: t('Booking started'), type: 'SUCCESS' });
          break;

        case type === WEBSOCKET_EVENT.CurrentOrganisationTimeBookings:
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingByProjectEntryAdded:
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingByProjectEntryRemoved:
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingByTagEntryRemoved:
          break;

        case type === WEBSOCKET_EVENT.UserTimeBookingByTagEntryAdded:
          //  Unhandled events
          break;
        default:
          break;
      }

      setLastMessageHash(stringHash(lastMessage));
    }
  }, [
    newMessage,
    lastMessageHash,
    lastMessage,
    mutate,
    selectedOrganisationId,
    mutateMany,
    addToast,
    t,
  ]);

  return <></>;
};
