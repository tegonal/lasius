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

import { addFavoriteBooking } from 'lib/api/lasius/user-favorites/user-favorites';
import {
  ModelsBooking,
  ModelsBookingStub,
  ModelsCurrentUserTimeBooking,
  ModelsTag,
} from 'lib/api/lasius';
import {
  deleteUserBooking,
  startUserBookingCurrent,
} from 'lib/api/lasius/user-bookings/user-bookings';
import { useStore } from 'storeContext/store';
import useModal from 'components/modal/hooks/useModal';
import { useToast } from 'components/toasts/hooks/useToast';
import { useTranslation } from 'next-i18next';

export const useContextMenu = () => {
  const { dispatch, state } = useStore();
  const { closeModal } = useModal('BookingAddMobileModal');
  const { addToast } = useToast();
  const { t } = useTranslation('common');

  const handleOpenContextMenu = (hash: string) => {
    dispatch({ type: 'context.open', payload: hash });
  };

  const handleCloseContextMenu = (hash: string) => {
    if (hash && state.contextMenuOpen === hash) {
      dispatch({ type: 'context.close' });
    }
  };

  const handleCloseAll = () => {
    dispatch({ type: 'context.close' });
  };

  const actionStartBooking = async (
    selectedOrganisationId: string,
    item: ModelsBooking | ModelsCurrentUserTimeBooking | ModelsBookingStub
  ) => {
    let projectId = '';
    let tags: ModelsTag[] = [];

    if ('projectReference' in item) {
      projectId = item.projectReference.id;
      tags = item.tags;
    }

    if ('booking' in item && item.booking) {
      projectId = item.booking.projectReference.id;
      tags = item.booking.tags;
    }

    await startUserBookingCurrent(selectedOrganisationId, { projectId, tags });
    handleCloseAll();
    closeModal();
  };

  const actionDeleteBooking = async (selectedOrganisationId: string, item: ModelsBooking) => {
    await deleteUserBooking(selectedOrganisationId, item.id);
    handleCloseAll();
  };

  const actionAddBookingToFavorites = async (
    selectedOrganisationId: string,
    item: ModelsBooking
  ) => {
    const {
      projectReference: { id: projectId },
      tags,
    } = item;
    await addFavoriteBooking(selectedOrganisationId, { projectId, tags });
    addToast({ message: t('Booking added to favorites'), type: 'SUCCESS' });
    handleCloseAll();
  };

  return {
    actionAddBookingToFavorites,
    handleOpenContextMenu,
    handleCloseContextMenu,
    actionStartBooking,
    actionDeleteBooking,
    currentOpenContextMenuId: state.contextMenuOpen,
    handleCloseAll,
  };
};
